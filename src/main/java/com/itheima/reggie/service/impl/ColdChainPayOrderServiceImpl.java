package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.api.request.DriverPayOrderPageQueryReq;
import com.itheima.reggie.api.response.DriverPayOrderPageVO;
import com.itheima.reggie.api.response.PageResult;
import com.itheima.reggie.entity.ColdChainPayOrderDO;
import com.itheima.reggie.enums.ColdChainPayStatusEnum;
import com.itheima.reggie.mapper.IColdChainPayOrderMapper;
import com.itheima.reggie.service.IColdChainPayOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 冷运定金支付单服务实现类
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainPayOrderServiceImpl implements IColdChainPayOrderService {

    /**
     * 默认页码
     */
    private static final Long DEFAULT_PAGE_NO = 1L;

    /**
     * 默认每页条数
     */
    private static final Long DEFAULT_PAGE_SIZE = 20L;

    /**
     * 单次查询最大条数。
     * <p>
     * 防止前端传入 100000，导致数据库和应用内存压力过大。
     */
    private static final Long MAX_PAGE_SIZE = 100L;

    /**
     * XXL-JOB 单批扫描最大数量
     */
    private static final Integer MAX_TIMEOUT_SCAN_LIMIT = 100;

    /**
     * 超时关闭原因
     */
    private static final String CLOSE_REASON_PAY_TIMEOUT = "PAY_TIMEOUT";

    /**
     * 冷运支付单 Mapper。
     * <p>
     * 该 Mapper 继承 BaseMapper，
     * 不需要 XML 也能调用 selectPage、update 等通用方法。
     */
    private final IColdChainPayOrderMapper coldChainPayOrderMapper;

    /**
     * 查询司机自己的支付单分页列表
     *
     * @param driverId 当前登录司机ID
     * @param request  查询条件
     * @return 支付单分页数据
     */
    @Override
    public PageResult<DriverPayOrderPageVO> queryDriverPayOrderPage(Long driverId, DriverPayOrderPageQueryReq request) {

        if (Objects.isNull(driverId)) {
            return PageResult.empty(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE);
        }

        Long pageNo = buildPageNo(request);
        Long pageSize = buildPageSize(request);

        Integer payStatus = request == null ? null : request.getPayStatus();
        String payOrderNo = request == null ? null : request.getPayOrderNo();

        /**
         * 前端传入状态时，必须先校验是否合法。
         *
         * 不建议把任意状态值直接带入数据库查询，
         * 这样后续状态体系扩展时不容易发现非法调用。
         */
        if (Objects.nonNull(payStatus) && !ColdChainPayStatusEnum.isValidCode(payStatus)) {
            throw new IllegalArgumentException("支付状态不合法");
        }

        LambdaQueryWrapper<ColdChainPayOrderDO> queryWrapper = getDriverPayOrderPageQueryWrapper(driverId, payStatus, payOrderNo);


        /**
         * MyBatis-Plus 分页对象。
         *
         * Page 的两个核心入参：
         * current：第几页，从 1 开始；
         * size：每页条数。
         */
        Page<ColdChainPayOrderDO> payOrderPage = new Page<>(pageNo, pageSize);

        /**
         * selectPage 会自动执行分页查询。
         *
         * 在 MySQL 下，最终会拼出 LIMIT。
         */
        Page<ColdChainPayOrderDO> resultPage = coldChainPayOrderMapper.selectPage(payOrderPage, queryWrapper);

        List<ColdChainPayOrderDO> payOrderList = resultPage.getRecords();
        if (CollectionUtils.isEmpty(payOrderList)) {
            return PageResult.empty(pageNo, pageSize);
        }

        List<DriverPayOrderPageVO> pageVOList = new ArrayList<>();

        // 遍历DO列表, 将每个DO转换成VO
        for (ColdChainPayOrderDO payOrderDO : payOrderList) {
            if (Objects.isNull(payOrderDO)) {
                continue;
            }

            DriverPayOrderPageVO pageVO = convertToPageVO(payOrderDO);
            if (Objects.nonNull(pageVO)) {
                pageVOList.add(pageVO);
            }
        }

        return new PageResult<>(pageNo, pageSize, resultPage.getTotal(), pageVOList);
    }


    /**
     * 查询超时但仍处于待支付状态的支付单
     *
     * @param lastPayOrderId 上一次扫描到的最大支付单ID
     * @param limit 单批最大扫描数量
     * @param currentTime 当前时间
     * @return 超时待支付单列表
     */
    @Override
    public List<ColdChainPayOrderDO> queryExpiredWaitPayOrderList(Long lastPayOrderId, Integer limit, LocalDateTime currentTime) {

        if (Objects.isNull(currentTime)) {
            return Collections.emptyList();
        }

        long safeLastPayOrderId = Objects.isNull(lastPayOrderId) ? 0L : lastPayOrderId;

        int safeLimit = Objects.isNull(limit) ?
                MAX_TIMEOUT_SCAN_LIMIT :
                Math.min(Math.max(limit, 1), MAX_TIMEOUT_SCAN_LIMIT);

        LambdaQueryWrapper<ColdChainPayOrderDO> queryWrapper = getExpiredWaitPayOrderListQueryWrapper(currentTime, safeLastPayOrderId, safeLimit);

        List<ColdChainPayOrderDO> payOrderList = coldChainPayOrderMapper.selectList(queryWrapper);

        return CollectionUtils.isEmpty(payOrderList) ? Collections.emptyList() : payOrderList;
    }

    /**
     * 超时关闭支付单
     *
     * 核心原则：
     * 不先查再更新，而是直接执行带状态条件的 UPDATE。
     *
     * @param payOrderId 支付单ID
     * @param currentTime 当前时间
     * @return true：本次真正关闭成功；false：已被其他流程处理
     */
    @Override
    public boolean closePayOrderIfWaitingAndExpired(Long payOrderId, LocalDateTime currentTime) {
        if (Objects.isNull(payOrderId) || Objects.isNull(currentTime)) {
            return false;
        }

        LambdaUpdateWrapper<ColdChainPayOrderDO> updateWrapper = getclosePayOrderIfWaitingAndExpiredUpdateWrapper(payOrderId, currentTime);
        int affectedRows = coldChainPayOrderMapper.update(null, updateWrapper);

        return affectedRows == 1;
    }

    /**
     * 将待支付单推进为已支付
     *
     * 调用前提：
     * 1. 已完成渠道验签；
     * 2. 已完成金额校验；
     * 3. 已确认支付渠道最终状态为成功。
     *
     * @param payOrderId 支付单ID
     * @param paidTime 支付成功时间
     * @return true：状态推进成功；false：订单已被其他流程处理
     */
    @Override
    public boolean markPayOrderPaidIfWaiting(Long payOrderId, LocalDateTime paidTime) {

        if (Objects.isNull(payOrderId) || Objects.isNull(paidTime)) {
            return false;
        }

        LambdaUpdateWrapper<ColdChainPayOrderDO> updateWrapper = getmarkPayOrderPaidIfWaitingUpdateWrapper(payOrderId, paidTime);

        int affectedRows = coldChainPayOrderMapper.update(null, updateWrapper);

        return affectedRows == 1;
    }

    private static LambdaQueryWrapper<ColdChainPayOrderDO> getExpiredWaitPayOrderListQueryWrapper(LocalDateTime currentTime, long safeLastPayOrderId, int safeLimit) {
        LambdaQueryWrapper<ColdChainPayOrderDO> queryWrapper = new LambdaQueryWrapper<>();

        /**
         * 只扫描仍待支付的支付单。
         */
        queryWrapper.eq(ColdChainPayOrderDO::getPayStatus, ColdChainPayStatusEnum.WAIT_PAY.getCode());

        /**
         * 只扫描已超过支付截止时间的支付单。
         */
        queryWrapper.le(ColdChainPayOrderDO::getPayExpireTime, currentTime);

        /**
         * 使用 ID 游标式扫描，避免传统 offset 深分页。
         *
         * 每次查询只拿上次最大 ID 之后的数据。
         */
        queryWrapper.gt(ColdChainPayOrderDO::getId, safeLastPayOrderId);

        queryWrapper.orderByAsc(ColdChainPayOrderDO::getId);

        /**
         * last 会直接拼接 SQL。
         *
         * 所以绝对不能把前端传入的字符串直接拼进 last。
         *
         * 当前 safeLimit 已经被限制在 1 ~ 100，
         * 因此这里是安全的固定数值。
         */
        queryWrapper.last("LIMIT " + safeLimit);
        return queryWrapper;
    }


    private static LambdaUpdateWrapper<ColdChainPayOrderDO> getmarkPayOrderPaidIfWaitingUpdateWrapper(Long payOrderId, LocalDateTime paidTime) {
        LambdaUpdateWrapper<ColdChainPayOrderDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(ColdChainPayOrderDO::getPayStatus, ColdChainPayStatusEnum.PAID.getCode());
        updateWrapper.set(ColdChainPayOrderDO::getPaidTime, paidTime);
        updateWrapper.set(ColdChainPayOrderDO::getUpdateTime, LocalDateTime.now());
        updateWrapper.eq(ColdChainPayOrderDO::getId, payOrderId);
        /**
         * 同样要求原状态必须为待支付。
         *
         * 支付回调重复到达时：
         * 第一次更新 1 行；
         * 第二次更新 0 行。
         *
         * 这就是支付回调幂等的一层基础保护。
         */
        updateWrapper.eq(ColdChainPayOrderDO::getPayStatus, ColdChainPayStatusEnum.WAIT_PAY.getCode());
        return updateWrapper;
    }

    /**
     * 最终 SQL 大致如下：
     *
     * UPDATE cold_chain_pay_order
     * SET pay_status = 30,
     *     close_reason = 'PAY_TIMEOUT',
     *     update_time = ?
     * WHERE id = ?
     *   AND pay_status = 10
     *   AND pay_expire_time <= ?
     *
     * affectedRows = 1：
     * 本次超时关闭真正成功。
     *
     * affectedRows = 0：
     * 可能已经支付、已经关闭、未到期，或者支付单不存在。
     */
    private static LambdaUpdateWrapper<ColdChainPayOrderDO> getclosePayOrderIfWaitingAndExpiredUpdateWrapper(Long payOrderId, LocalDateTime currentTime) {
        LambdaUpdateWrapper<ColdChainPayOrderDO> updateWrapper = new LambdaUpdateWrapper<>();

        /**
         * 设置关闭后的状态。
         */
        updateWrapper.set(ColdChainPayOrderDO::getPayStatus, ColdChainPayStatusEnum.CLOSED.getCode());

        /**
         * 记录关闭原因，方便客服后台、审计和问题排查。
         */
        updateWrapper.set(ColdChainPayOrderDO::getCloseReason, CLOSE_REASON_PAY_TIMEOUT);

        /**
         * 当前 update 使用的是 update(null, wrapper)。
         *
         * 为了让更新时间在该更新路径上明确可控，
         * 这里直接设置 updateTime。
         */
        updateWrapper.set(ColdChainPayOrderDO::getUpdateTime, currentTime);

        /**
         * 只更新指定支付单。
         */
        updateWrapper.eq(ColdChainPayOrderDO::getId, payOrderId);

        /**
         * 关键条件一：
         *
         * 只有待支付状态才能被超时任务关闭。
         *
         * 如果支付回调已经先把状态改为 PAID，
         * 这里 SQL 将影响 0 行。
         */
        updateWrapper.eq(ColdChainPayOrderDO::getPayStatus, ColdChainPayStatusEnum.WAIT_PAY.getCode());

        /**
         * 关键条件二：
         *
         * 只有真正到期的支付单才能被关闭。
         */
        updateWrapper.le(ColdChainPayOrderDO::getPayExpireTime, currentTime);
        return updateWrapper;
    }

    /**
     * 获取司机自己的支付单分页queryWrapper
     * LambdaQueryWrapper 的价值：
     * 1. 使用实体属性，不硬编码数据库字段名；
     * 2. ColdChainPayOrderDO::getDriverId 改名时，IDE 可以辅助发现；
     * 3. 自动拼接 WHERE 条件；
     * 4. 不需要 XML 中的 <if> 动态 SQL。
     */
    private static LambdaQueryWrapper<ColdChainPayOrderDO> getDriverPayOrderPageQueryWrapper(Long driverId, Integer payStatus, String payOrderNo) {

        LambdaQueryWrapper<ColdChainPayOrderDO> queryWrapper = new LambdaQueryWrapper<>();

        /**
         * 只查询司机自己的支付单。
         * driverId 必须来自登录态、网关透传或用户中心，
         * 不能相信前端请求体里随便传的 driverId。
         */
        queryWrapper.eq(ColdChainPayOrderDO::getDriverId, driverId);

        /**
         * payStatus 不为空时才拼接状态过滤条件。
         * 等价 SQL：AND pay_status = ?
         */
        queryWrapper.eq(Objects.nonNull(payStatus), ColdChainPayOrderDO::getPayStatus, payStatus);

        /**
         * 支付单号前缀模糊查询。
         *
         * likeRight("PAY2026")：
         * pay_order_no LIKE 'PAY2026%'
         *
         * 前缀匹配比 '%PAY2026%' 更容易利用索引。
         */
        queryWrapper.likeRight(StringUtils.hasText(payOrderNo), ColdChainPayOrderDO::getPayOrderNo, payOrderNo);

        /**
         * 只查询司机端真正需要的字段。
         *
         * 不返回未来可能存在的：
         * 渠道流水号、验签信息、内部风控字段等敏感信息。
         */
        queryWrapper.select(
                ColdChainPayOrderDO::getId,
                ColdChainPayOrderDO::getPayOrderNo,
                ColdChainPayOrderDO::getBusinessOrderId,
                ColdChainPayOrderDO::getCargoId,
                ColdChainPayOrderDO::getPayAmount,
                ColdChainPayOrderDO::getPayStatus,
                ColdChainPayOrderDO::getPayExpireTime,
                ColdChainPayOrderDO::getPaidTime,
                ColdChainPayOrderDO::getCreateTime
        );

        /**
         * 司机端优先展示最新创建的支付单。
         */
        queryWrapper.orderByDesc(ColdChainPayOrderDO::getCreateTime);

        return queryWrapper;
    }

    /**
     * 标准化页码
     *
     * @param request 分页请求
     * @return 合法页码
     */
    private Long buildPageNo(DriverPayOrderPageQueryReq request) {
        if (Objects.isNull(request) || Objects.isNull(request.getPageNo()) || request.getPageNo() < 1L) {
            return DEFAULT_PAGE_NO;
        }

        return request.getPageNo();
    }

    /**
     * 标准化每页条数
     *
     * @param request 分页请求
     * @return 合法每页条数
     */
    private Long buildPageSize(DriverPayOrderPageQueryReq request) {
        if (Objects.isNull(request) || Objects.isNull(request.getPageSize()) || request.getPageSize() < 1L) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.min(request.getPageSize(), MAX_PAGE_SIZE);
    }

    /**
     * 数据库实体转换为司机端分页 VO
     *
     * @param payOrderDO 支付单数据库实体
     * @return 司机端支付单展示对象
     */
    private DriverPayOrderPageVO convertToPageVO(ColdChainPayOrderDO payOrderDO) {
        if (Objects.isNull(payOrderDO)) {
            return null;
        }

        DriverPayOrderPageVO pageVO = new DriverPayOrderPageVO();

        pageVO.setPayOrderId(payOrderDO.getId());
        pageVO.setPayOrderNo(payOrderDO.getPayOrderNo());
        pageVO.setBusinessOrderId(payOrderDO.getBusinessOrderId());
        pageVO.setCargoId(payOrderDO.getCargoId());
        pageVO.setPayAmount(payOrderDO.getPayAmount());
        pageVO.setPayStatus(payOrderDO.getPayStatus());
        pageVO.setPayExpireTime(payOrderDO.getPayExpireTime());
        pageVO.setPaidTime(payOrderDO.getPaidTime());
        pageVO.setCreateTime(payOrderDO.getCreateTime());

        /**
         * 将状态编码转换为前端可展示文本。
         */
        pageVO.setPayStatusDesc(ColdChainPayStatusEnum.getDescByCode(payOrderDO.getPayStatus()));

        return pageVO;
    }
}
