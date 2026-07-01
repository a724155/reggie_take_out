package com.itheima.reggie.service.impl;

import com.itheima.reggie.common.ColdChainBusinessException;
import com.itheima.reggie.common.ColdChainCouponErrorCodeConstants;
import com.itheima.reggie.entity.ColdChainCouponTemplateDO;
import com.itheima.reggie.entity.ColdChainDriverCouponDO;
import com.itheima.reggie.enums.ColdChainCouponStatusEnum;
import com.itheima.reggie.mapper.IColdChainCouponTemplateMapper;
import com.itheima.reggie.mapper.IColdChainDriverCouponMapper;
import com.itheima.reggie.service.IColdChainDriverCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;


/**
 * 冷运司机优惠券服务实现。
 * <p>
 * 领取优惠券是一个典型的“库存 + 用户权益”场景。
 * <p>
 * 必须保证：
 * <p>
 * 1. 模板库存扣减成功；
 * 2. 司机优惠券新增成功；
 * <p>
 * 两件事要么都成功，要么都失败。
 * <p>
 * 所以需要数据库事务。
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainDriverCouponServiceImpl implements IColdChainDriverCouponService {

    // 模板启用
    private static final int TEMPLATE_START = 1;

    // 模板停用
    private static final int TEMPLATE_STOP = 0;

    private final IColdChainCouponTemplateMapper coldChainCouponTemplateMapper;

    private final IColdChainDriverCouponMapper coldChainDriverCouponMapper;

    /**
     * 司机领取优惠券。
     * <p>
     * 执行顺序：
     * <p>
     * 1. 校验司机 ID、模板 ID；
     * 2. 查询优惠券模板；
     * 3. 校验模板基础规则；
     * 4. 查询司机是否已领取；
     * 5. 使用条件 UPDATE 原子扣减库存；
     * 6. 生成司机优惠券快照；
     * 7. 插入司机优惠券记录；
     *
     * @param driverId   当前登录司机 ID
     * @param templateId 优惠券模板 ID
     * @return 司机优惠券 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long claimCoupon(Long driverId, Long templateId) {

        if (Objects.isNull(driverId) || driverId <= 0) {
            log.info("当前司机身份非法");
            return null;
        }

        if (Objects.isNull(templateId) || templateId <= 0) {
            log.info("优惠券模板ID非法");
            return null;
        }
        LocalDateTime currentTime = LocalDateTime.now();

        /*
          查询模板。
          作用：读取优惠规则，生成司机优惠券快照。
          注意：这个查询只能用于业务校验和错误提示。真正的库存、状态、领取时间校验，仍然必须放进后面的条件更新 SQL 中。
         */
        ColdChainCouponTemplateDO templateDO = coldChainCouponTemplateMapper.selectById(templateId);
        if (Objects.isNull(templateDO)) {
            log.info("优惠券模板不存在");
            return null;
        }

        validateCouponTemplate(templateDO, currentTime);

        /*
          先查询一次，给司机友好提示。但此处不能作为最终防重复保障。
          因为两个并发请求可能同时查到“未领取”。最终防重由数据库唯一索引保证：uk_template_driver(template_id, driver_id)
         */
        ColdChainDriverCouponDO existedDriverCouponDO = coldChainDriverCouponMapper.selectByTemplateIdAndDriverId(templateId, driverId);
        if (!Objects.isNull(existedDriverCouponDO)) {
            throw new ColdChainBusinessException("您已经领取过该优惠券");
        }

        /*
          原子扣减优惠券模板库存。
          SQL 中已经包含：
          remain_count > 0
          template_status = 1
          领取时间范围校验
          返回 1：扣减成功。
          返回 0：库存不足、券被停用或不在领取时间。
         */
        int decreaseCount = coldChainCouponTemplateMapper.decreaseRemainCountForClaim(templateId, currentTime);
        if (decreaseCount != 1) {
            throw new ColdChainBusinessException("优惠券已领完或当前不可领取");
        }

        /**
         * 创建司机优惠券。这里保存的是“快照”。
         * 即使运营之后修改了模板金额，已领取优惠券的金额和门槛也不会被修改。
         */
        ColdChainDriverCouponDO driverCouponDO = new ColdChainDriverCouponDO();
        driverCouponDO.setTemplateId(templateDO.getId());
        driverCouponDO.setDriverId(driverId);
        driverCouponDO.setCouponName(templateDO.getCouponName());
        driverCouponDO.setDiscountAmount(templateDO.getDiscountAmount());
        driverCouponDO.setThresholdAmount(templateDO.getThresholdAmount());
        driverCouponDO.setValidStartTime(currentTime);
        driverCouponDO.setValidEndTime(currentTime.plusDays(templateDO.getValidDays()));
        driverCouponDO.setCouponStatus(ColdChainCouponStatusEnum.UNUSED.getCode());

        try {
            int insertCount = coldChainDriverCouponMapper.insertDriverCoupon(driverCouponDO);
            if (insertCount != 1 || Objects.isNull(driverCouponDO.getId())) {
                throw new ColdChainBusinessException("领取优惠券失败");
            }
            return driverCouponDO.getId();
        } catch (DuplicateKeyException exception) {
            /**
             * 并发领取时的最终兜底。
             * 例如：
             * 请求 A 和请求 B 同时判断“司机未领取”；
             * 请求 A 插入成功；
             * 请求 B 触发 uk_template_driver 唯一索引冲突。
             * 请求 B 抛出运行时异常后，当前事务整体回滚。前面扣减的 remain_count 会自动恢复，不会发生“库存少了，但司机没领到券”的资损。
             */
            throw new ColdChainBusinessException("您已经领取过该优惠券");
        }

    }


    /**
     * 校验优惠券模板基础配置。
     *
     * @param templateDO  优惠券模板
     * @param currentTime 当前时间
     */
    private void validateCouponTemplate(ColdChainCouponTemplateDO templateDO, LocalDateTime currentTime) {

        if (!Objects.equals(templateDO.getTemplateStatus(), TEMPLATE_START)) {
            throw new ColdChainBusinessException(ColdChainCouponErrorCodeConstants.COUPON_NOT_CLAIMABLE, "优惠券当前不可领取");
        }

        if (templateDO.getDiscountAmount() == null || templateDO.getDiscountAmount().signum() <= 0) {
            throw new ColdChainBusinessException(ColdChainCouponErrorCodeConstants.COUPON_AMOUNT_CONFIGURATION_ERROR, "优惠券金额配置异常");
        }

        if (templateDO.getThresholdAmount() == null || templateDO.getThresholdAmount().signum() < 0) {
            throw new ColdChainBusinessException(ColdChainCouponErrorCodeConstants.COUPON_TEMPLATE_NOT_FOUND, "优惠券门槛配置异常");
        }

        if (templateDO.getValidDays() == null || templateDO.getValidDays() <= 0) {
            throw new ColdChainBusinessException(ColdChainCouponErrorCodeConstants.COUPON_VALIDITY_PERIOD_CONFIGURATION_ERROR, "优惠券有效期配置异常");
        }

        if (templateDO.getReceiveStartTime() == null || templateDO.getReceiveEndTime() == null) {
            throw new ColdChainBusinessException(ColdChainCouponErrorCodeConstants.COUPON_REDEMPTION_TIME_CONFIGURATION_ERROR, "优惠券领取时间配置异常");
        }

        if (templateDO.getReceiveStartTime().isAfter(currentTime) || !templateDO.getReceiveEndTime().isAfter(currentTime)) {
            throw new ColdChainBusinessException(ColdChainCouponErrorCodeConstants.COUPON_NO_PERIOD, "当前不在优惠券领取时间内");
        }
    }
}
