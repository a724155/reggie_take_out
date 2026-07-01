package com.itheima.reggie.mapper;


import com.itheima.reggie.entity.ColdChainCouponTemplateDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 冷运优惠券模板 Mapper。
 *
 * 负责操作：
 * cold_chain_coupon_template
 *
 * 注意：
 * 当前使用原生 MyBatis XML。
 * SQL 不写在接口注解里，全部写在对应 XML 中。
 */
public interface IColdChainCouponTemplateMapper {

    /**
     * 根据优惠券模板 ID 查询模板。
     *
     * 当前主要用于：
     * 1. 领取优惠券前，读取优惠券规则；
     * 2. 校验模板状态、优惠金额、门槛、有效期；
     * 3. 生成司机优惠券时，保存模板快照。
     *
     * @param templateId 优惠券模板 ID
     * @return 优惠券模板；不存在时返回 null
     */
    ColdChainCouponTemplateDO selectById(@Param("templateId") Long templateId);

    /**
     * 原子扣减优惠券库存。
     * 该 SQL 的核心条件是：remain_count > 0
     * 多个司机同时领取最后一张券时：
     * 司机 A 更新成功，返回 1；
     * 司机 B 更新失败，返回 0；
     * 这样不会发生库存扣成负数，也不会超发。
     * @param templateId 优惠券模板 ID
     * @param currentTime 当前时间
     * @return 1：扣减成功；0：库存不足、模板停用或不在领取时间内
     */
    int decreaseRemainCountForClaim(@Param("templateId") Long templateId, @Param("currentTime") LocalDateTime currentTime);
}
