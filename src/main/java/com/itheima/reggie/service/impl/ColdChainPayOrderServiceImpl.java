package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.api.request.ColdChainCreatePayOrderReq;
import com.itheima.reggie.api.request.DriverPayOrderPageQueryReq;
import com.itheima.reggie.api.response.ColdChainCreatePayOrderVO;
import com.itheima.reggie.api.response.DriverPayOrderPageVO;
import com.itheima.reggie.common.ColdChainBusinessException;
import com.itheima.reggie.common.OrderPageResult;
import com.itheima.reggie.entity.ColdChainDriverCouponDO;
import com.itheima.reggie.entity.ColdChainOrderDO;
import com.itheima.reggie.entity.ColdChainPayOrderDO;
import com.itheima.reggie.enums.ColdChainCouponStatusEnum;
import com.itheima.reggie.enums.ColdChainOrderStatusEnum;
import com.itheima.reggie.enums.ColdChainPayStatusEnum;
import com.itheima.reggie.mapper.IColdChainDriverCouponMapper;
import com.itheima.reggie.mapper.IColdChainOrderMapper;
import com.itheima.reggie.mapper.IColdChainPayOrderMapper;
import com.itheima.reggie.mapper.IColdChainPayOrderMapperPlus;
import com.itheima.reggie.service.IColdChainPayOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 冷运定金支付单服务实现类
 */

/**
 * 冷运支付单服务实现。
 *
 * 这部分是优惠券防资损的核心。
 *
 * 创建支付单时：
 *
 * 订单加锁
 *     ↓
 * 校验司机归属
 *     ↓
 * 校验订单状态
 *     ↓
 * 校验 / 锁定优惠券
 *     ↓
 * 服务端计算优惠金额
 *     ↓
 * 创建支付单
 *
 * 支付成功时：
 *
 * 验签完成
 *     ↓
 * 校验金额
 *     ↓
 * 支付单 WAIT_PAY -> PAID
 *     ↓
 * 优惠券 LOCKED -> USED
 *     ↓
 * 订单 WAIT_DEPOSIT_PAY -> DEPOSIT_PAID
 *
 * 上面三个状态变更必须处于同一个事务中。
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
    private final IColdChainPayOrderMapperPlus iColdChainPayOrderMapperPlus;

    /**
     * 支付单有效期。
     *
     * 当前练习设置为 10 分钟。
     */
    private static final long PAY_EXPIRE_MINUTES = 10L;

    /**
     * 支付单号时间格式。
     */
    private static final DateTimeFormatter PAY_ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final IColdChainOrderMapper coldChainOrderMapper;

    private final IColdChainDriverCouponMapper coldChainDriverCouponMapper;

    private final IColdChainPayOrderMapper coldChainPayOrderMapper;


    /**
     * 查询司机自己的支付单分页列表
     *
     * @param driverId 当前登录司机ID
     * @param request  查询条件
     * @return 支付单分页数据
     */
    @Override
    public OrderPageResult<DriverPayOrderPageVO> queryDriverPayOrderPage(Long driverId, DriverPayOrderPageQueryReq request) {

        if (Objects.isNull(driverId)) {
            return OrderPageResult.empty(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE);
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
        Page<ColdChainPayOrderDO> resultPage = iColdChainPayOrderMapperPlus.selectPage(payOrderPage, queryWrapper);

        List<ColdChainPayOrderDO> payOrderList = resultPage.getRecords();
        if (CollectionUtils.isEmpty(payOrderList)) {
            return OrderPageResult.empty(pageNo, pageSize);
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

        OrderPageResult<DriverPayOrderPageVO> driverPayOrderPageVOOrderPageResult = new OrderPageResult<>(pageNo, pageSize, resultPage.getTotal(), pageVOList);

        return driverPayOrderPageVOOrderPageResult;
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

        List<ColdChainPayOrderDO> payOrderList = iColdChainPayOrderMapperPlus.selectList(queryWrapper);

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
        int affectedRows = iColdChainPayOrderMapperPlus.update(null, updateWrapper);

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

        int affectedRows = iColdChainPayOrderMapperPlus.update(null, updateWrapper);

        return affectedRows == 1;
    }



    /**
     * 创建支付单。
     *
     * 关键防护：
     * 1. 当前司机 ID 来自拦截器，不信任前端；
     * 2. 原始定金从订单表读取，不信任前端；
     * 3. 优惠金额由后端计算，不信任前端；
     * 4. 司机优惠券必须属于当前司机；
     * 5. 优惠券使用条件更新完成原子锁定；
     * 6. 订单行加锁，避免重复创建支付单；
     * 7. 数据库唯一索引做最终幂等兜底。
     *
     * @param driverId 当前登录司机 ID
     * @param orderId 冷运订单 ID
     * @param request 创建支付单请求
     * @return 支付单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ColdChainCreatePayOrderVO createPayOrder(Long driverId, Long orderId, ColdChainCreatePayOrderReq request) {

        if (driverId == null || driverId <= 0) {
            throw new ColdChainBusinessException("当前司机身份非法");
        }
        //TODO 是我
        if (orderId == null || orderId <= 0) {
            throw new ColdChainBusinessException("订单ID非法");
        }

        if (Objects.isNull(request) || StringUtils.isBlank(request.getRequestNo()) || StringUtils.isBlank(request.getRequestNo().trim())) {
            throw new ColdChainBusinessException("请求幂等号不能为空");
        }

        String requestNo = request.getRequestNo().trim();

        /**
         * 第一层幂等：
         * 同一个 requestNo 重试，直接返回历史支付单。
         * 前端必须做到：第一次点击“去支付”生成 UUID；网络超时重试时继续使用同一个 UUID；不能每次重试都生成新的 requestNo。
         */
        ColdChainPayOrderDO existedRequestPayOrderDO = coldChainPayOrderMapper.selectByRequestNo(requestNo);
        if (!Objects.isNull(existedRequestPayOrderDO)) {
            validateExistingPayOrder(existedRequestPayOrderDO, driverId, orderId);
            return buildCreatePayOrderVO(existedRequestPayOrderDO);
        }

        /*
          锁订单行。这一步非常重要。同一订单的多个创建支付单请求，必须串行执行。
         */
        ColdChainOrderDO orderDO = coldChainOrderMapper.selectByIdForUpdate(orderId);
        if (Objects.isNull(orderDO)) {
            throw new ColdChainBusinessException("订单不存在");
        }

        /**
         * 防止水平越权。
         * 司机 A 不能通过修改 URL 里的 orderId，为司机 B 的订单创建支付单。
         */
        if (!Objects.equals(orderDO.getDriverId(), driverId)) {
            throw new ColdChainBusinessException("无权操作该订单");
        }

        if (!Objects.equals(orderDO.getOrderStatus(), ColdChainOrderStatusEnum.WAIT_DEPOSIT_PAY.getCode())) {
            throw new ColdChainBusinessException("当前订单不允许创建支付单");
        }

        if (orderDO.getDepositAmount() == null || orderDO.getDepositAmount().signum() <= 0) {
            throw new ColdChainBusinessException("订单定金金额异常");
        }

        /**
         * 第二层幂等：
         * 即使用户换了一个 requestNo，同一个订单也不能创建第二笔支付单。
         */
        ColdChainPayOrderDO existedOrderPayOrderDO = coldChainPayOrderMapper.selectByOrderId(orderId);
        if (existedOrderPayOrderDO != null) {

            /*
              当前练习约束：
              一个订单支付单关闭后，不允许直接再创建。真实业务里可能需要重新抢单、重新锁货，再生成新订单或新支付单。
             */
            if (Objects.equals(existedOrderPayOrderDO.getPayStatus(), ColdChainPayStatusEnum.CLOSED.getCode())) {
                throw new ColdChainBusinessException("支付单已关闭，请重新发起业务流程");
            }

            return buildCreatePayOrderVO(existedOrderPayOrderDO);
        }

        BigDecimal originalAmount = orderDO.getDepositAmount();
        /*
          不使用优惠券时，优惠金额固定为 0。
         */
        BigDecimal couponDiscountAmount = BigDecimal.ZERO;
        Long driverCouponId = request.getDriverCouponId();
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime expireTime = currentTime.plusMinutes(PAY_EXPIRE_MINUTES);

        /*
          当前司机选择使用优惠券。
         */
        if (driverCouponId != null) {

            /**
             * 读取优惠券快照。
             * 注意：这一步只用于读取金额、做友好校验。
             * 真正防并发的一步，是后面的 UPDATE ... WHERE coupon_status = 10。
             */
            ColdChainDriverCouponDO driverCouponDO = coldChainDriverCouponMapper.selectById(driverCouponId);

            validateCouponCanBeUsed(driverCouponDO, driverId, originalAmount, currentTime);

            /**
             * 优惠金额必须由服务端计算。
             *
             * 前端只允许传：
             * driverCouponId
             *
             * 前端绝不能传：
             * couponDiscountAmount
             * payAmount
             * originalAmount
             *
             * 否则用户可以通过抓包，把：
             * payAmount = 8
             * 篡改为：
             * payAmount = 0.01
             */
            couponDiscountAmount = calculateCouponDiscount(driverCouponDO, originalAmount);

            /**
             * 原子锁券。
             *
             * 这里才是同一张券防重复使用的最终保障。
             *
             * 两个订单同时使用同一张券时：
             *
             * 第一个 UPDATE 成功，返回 1；
             * 第二个 UPDATE 因 coupon_status 已变为 LOCKED，返回 0。
             */
            int lockCouponCount = coldChainDriverCouponMapper.lockCouponForPay(
                    driverCouponId,
                    driverId,
                    orderId,
                    originalAmount,
                    expireTime,
                    currentTime
            );

            if (lockCouponCount != 1) {
                throw new ColdChainBusinessException("优惠券已被使用、锁定、过期或不满足使用条件");
            }
        }
        /**
         * 实付金额 = 原始定金 - 服务端计算出的优惠金额。
         */
        BigDecimal payAmount = originalAmount.subtract(couponDiscountAmount);

        /**
         * 当前练习暂不实现零元支付。所以要求优惠后金额必须大于 0。
         * 后续扩展零元支付时：不需要调第三方支付渠道；直接走“支付成功 + 核销券 + 推进订单”的本地事务。
         */
        if (payAmount.signum() <= 0) {
            throw new ColdChainBusinessException("优惠金额异常，当前暂不支持零元支付");
        }

        ColdChainPayOrderDO payOrderDO = new ColdChainPayOrderDO();
        payOrderDO.setPayOrderNo(generatePayOrderNo());
        payOrderDO.setRequestNo(requestNo);
        payOrderDO.setOrderId(orderId);
        payOrderDO.setDriverId(driverId);
        payOrderDO.setOriginalAmount(originalAmount);
        payOrderDO.setDriverCouponId(driverCouponId);
        payOrderDO.setCouponDiscountAmount(couponDiscountAmount);
        payOrderDO.setPayAmount(payAmount);
        payOrderDO.setPayStatus(ColdChainPayStatusEnum.WAIT_PAY.getCode());
        payOrderDO.setExpireTime(expireTime);

        try {
            int insertCount = coldChainPayOrderMapper.insertPayOrder(payOrderDO);
            if (insertCount != 1 || payOrderDO.getId() == null) {
                throw new ColdChainBusinessException("创建支付单失败");
            }
        } catch (DuplicateKeyException exception) {

            /**
             * 这里主要兜底极端并发场景：
             *
             * 1. requestNo 被重复使用；
             * 2. orderId 并发创建支付单；
             * 3. payOrderNo 极小概率重复。
             *
             * 当前事务会回滚。
             *
             * 如果前面已经锁券，
             * 锁券状态也会一起回滚，
             * 不会遗留“券锁住但支付单没创建”的脏数据。
             */
            throw new ColdChainBusinessException("创建支付单失败，请勿重复提交");
        }

        return buildCreatePayOrderVO(payOrderDO);

    }

    /**
     * 支付渠道验签成功后，处理支付成功。
     *
     * 调用本方法前，调用方必须完成：
     *
     * 1. 支付渠道签名校验；
     * 2. 商户号校验；
     * 3. 支付单号校验；
     * 4. 回调参数防篡改校验。
     *
     * 本方法只处理本地数据库状态。
     *
     * @param payOrderNo 商户支付单号
     * @param channelTradeNo 第三方支付渠道流水号
     * @param callbackPayAmount 第三方实际回调金额
     * @param channelPaidTime 第三方支付成功时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(String payOrderNo, String channelTradeNo, BigDecimal callbackPayAmount, LocalDateTime channelPaidTime) {

        if (isBlank(payOrderNo)) {
            throw new ColdChainBusinessException("商户支付单号不能为空");
        }

        if (isBlank(channelTradeNo)) {
            throw new ColdChainBusinessException("支付渠道流水号不能为空");
        }

        if (Objects.isNull(callbackPayAmount) || callbackPayAmount.signum() < 0) {
            throw new ColdChainBusinessException("支付回调金额非法");
        }

        if (channelPaidTime == null) {
            throw new ColdChainBusinessException("支付渠道成功时间不能为空");
        }

        /**
         * 锁定支付单行。解决支付回调和超时关闭任务并发问题。
         * 同一时刻：回调线程要把支付单改为 PAID；超时任务要把支付单改为 CLOSED；
         * 两者必须串行处理。
         */
        ColdChainPayOrderDO payOrderDO = coldChainPayOrderMapper.selectByPayOrderNoForUpdate(payOrderNo.trim());
        if (payOrderDO == null) {
            throw new ColdChainBusinessException("支付单不存在");
        }

        /**
         * 支付渠道可能重复回调。
         * 已支付支付单直接做幂等校验后返回，
         * 不再重复核销优惠券、重复推进订单。
         */
        if (Objects.equals(payOrderDO.getPayStatus(), ColdChainPayStatusEnum.PAID.getCode())) {
            validatePayAmount(payOrderDO, callbackPayAmount);
            if (!isBlank(payOrderDO.getChannelTradeNo()) && !Objects.equals(payOrderDO.getChannelTradeNo(), channelTradeNo.trim())) {
                throw new ColdChainBusinessException("支付渠道流水号与历史记录不一致");
            }
            return;
        }

        /*
          已关闭支付单又收到成功回调，属于严重异常场景。
          不能直接把 CLOSED 改回 PAID，需要进入支付对账、人工补偿或退款流程。
         */
        if (Objects.equals(payOrderDO.getPayStatus(), ColdChainPayStatusEnum.CLOSED.getCode())) {
            throw new ColdChainBusinessException("支付单已关闭，需要进入支付对账流程");
        }

        if (!Objects.equals(payOrderDO.getPayStatus(), ColdChainPayStatusEnum.WAIT_PAY.getCode())) {
            throw new ColdChainBusinessException("支付单状态异常");
        }

        validatePayAmount(payOrderDO, callbackPayAmount);

        /**
         * 回调到达时间晚，不等于司机支付晚。
         * 所以这里比较的是：
         * 支付渠道记录的实际支付成功时间
         * 而不是 Controller 收到回调的当前时间。
         */
        if (payOrderDO.getExpireTime() == null || channelPaidTime.isAfter(payOrderDO.getExpireTime())) {
            throw new ColdChainBusinessException("支付成功时间已超过支付单有效期，需要进入对账流程");
        }
        int paySuccessCount;
        try {
            paySuccessCount = coldChainPayOrderMapper.markPaySuccessIfWaiting(payOrderDO.getId(), channelTradeNo.trim(), channelPaidTime);
        } catch (DuplicateKeyException exception) {

            /**
             * 常见于异常渠道流水重复。
             * 例如同一 channelTradeNo 被试图写入两笔支付单。
             * 数据库唯一索引 uk_channel_trade_no 会兜住。
             */
            throw new ColdChainBusinessException("支付渠道流水号冲突，需要进入支付对账流程");
        }
        if (paySuccessCount != 1) {
            throw new ColdChainBusinessException("支付单状态更新失败");
        }

        /**
         * 有优惠券时，支付成功必须核销券。
         * 支付单 PAID、优惠券 USED、订单 DEPOSIT_PAID
         * 三者必须是一个事务。
         */
        if (payOrderDO.getDriverCouponId() != null) {
            int consumeCouponCount = coldChainDriverCouponMapper.consumeLockedCoupon(
                            payOrderDO.getDriverCouponId(), payOrderDO.getDriverId(),
                            payOrderDO.getOrderId(), channelPaidTime);

            if (consumeCouponCount != 1) {

                /**
                 * 不能只打日志然后继续。
                 * 否则可能出现：钱支付成功；订单已经成功；
                 * 但优惠券还处于 LOCKED。这会造成账实不一致。
                 * 抛异常后事务整体回滚，等支付渠道重试回调或人工补偿。
                 */
                throw new ColdChainBusinessException("优惠券核销失败，需要进入异常处理流程");
            }
        }

        int orderUpdatedCount = coldChainOrderMapper.updateOrderStatus(
                        payOrderDO.getOrderId(), payOrderDO.getDriverId(),
                        ColdChainOrderStatusEnum.WAIT_DEPOSIT_PAY.getCode(), ColdChainOrderStatusEnum.DEPOSIT_PAID.getCode());

        if (orderUpdatedCount != 1) {
            throw new ColdChainBusinessException("订单状态更新失败，需要进入异常处理流程");
        }

    }

    /**
     * 支付超时后关闭支付单并释放优惠券。
     * 注意：
     * 该方法不能由 XXL-JOB 一扫到超时单就直接调用。
     * 正确流程是：
     * 1. XXL-JOB 查询本地超时待支付单；
     * 2. 调用支付渠道查询接口；
     * 3. 渠道已支付，则调用 handlePaySuccess；
     * 4. 渠道确认未支付，才调用当前方法；
     *
     * @param payOrderId 支付单 ID
     */
    @Override
    public void closeExpiredPayOrderAfterChannelConfirmedUnpaid(Long payOrderId) {
        if (payOrderId == null || payOrderId <= 0) {
            return;
        }

        /**
         * 锁支付单。防止支付回调与超时任务同时修改支付单状态。
         */
        ColdChainPayOrderDO payOrderDO = coldChainPayOrderMapper.selectByIdForUpdate(payOrderId);

        if (payOrderDO == null) {
            return;
        }

        if (!Objects.equals(payOrderDO.getPayStatus(), ColdChainPayStatusEnum.WAIT_PAY.getCode())) {
            return;
        }

        LocalDateTime currentTime = LocalDateTime.now();

        if (payOrderDO.getExpireTime() == null || payOrderDO.getExpireTime().isAfter(currentTime)) {
            return;
        }

        int closePayOrderCount = coldChainPayOrderMapper.closePayOrderIfExpired(payOrderId, currentTime);

        if (closePayOrderCount != 1) {
            return;
        }

        /**
         * 有优惠券时，支付单关闭后必须释放优惠券。
         * LOCKED -> UNUSED
         */
        if (payOrderDO.getDriverCouponId() != null) {
            int releaseCouponCount = coldChainDriverCouponMapper.releaseLockedCoupon(payOrderDO.getDriverCouponId(), payOrderDO.getDriverId(), payOrderDO.getOrderId());
            if (releaseCouponCount != 1) {
                throw new ColdChainBusinessException("支付超时释放优惠券失败，需要进入异常处理流程");
            }
        }

        /**
         * 支付单关闭后，订单进入支付超时状态。
         * 后续你做锁货时，可以在这里继续：
         * 释放货源锁；
         * 恢复货源可见；
         * 通知司机；
         * 写操作日志；
         * 发送 RocketMQ 消息。
         */
        int orderUpdatedCount = coldChainOrderMapper.updateOrderStatus(
                        payOrderDO.getOrderId(), payOrderDO.getDriverId(),
                        ColdChainOrderStatusEnum.WAIT_DEPOSIT_PAY.getCode(),
                        ColdChainOrderStatusEnum.PAY_TIMEOUT.getCode());

        if (orderUpdatedCount != 1) {
            throw new ColdChainBusinessException("支付超时订单状态更新失败");
        }
    }

    /**
     * 校验司机优惠券是否可用。
     *
     * @param driverCouponDO 司机优惠券
     * @param driverId 当前司机 ID
     * @param orderAmount 当前订单定金金额
     * @param currentTime 当前时间
     */
    private void validateCouponCanBeUsed(ColdChainDriverCouponDO driverCouponDO, Long driverId,
            BigDecimal orderAmount, LocalDateTime currentTime) {

        if (driverCouponDO == null) {
            throw new ColdChainBusinessException("优惠券不存在");
        }

        if (!Objects.equals(driverCouponDO.getDriverId(), driverId)) {
            throw new ColdChainBusinessException("无权使用该优惠券");
        }

        if (!Objects.equals(driverCouponDO.getCouponStatus(), ColdChainCouponStatusEnum.UNUSED.getCode())) {
            throw new ColdChainBusinessException("优惠券当前不可使用");
        }

        if (driverCouponDO.getValidStartTime() == null || driverCouponDO.getValidEndTime() == null) {
            throw new ColdChainBusinessException("优惠券有效期配置异常");
        }

        if (driverCouponDO.getValidStartTime().isAfter(currentTime) || !driverCouponDO.getValidEndTime().isAfter(currentTime)) {
            throw new ColdChainBusinessException("优惠券未生效或已过期");
        }

        if (driverCouponDO.getThresholdAmount() == null || driverCouponDO.getDiscountAmount() == null) {
            throw new ColdChainBusinessException("优惠券金额配置异常");
        }

        if (driverCouponDO.getThresholdAmount().compareTo(orderAmount) > 0) {
            throw new ColdChainBusinessException("订单金额未达到优惠券使用门槛");
        }
    }

    /**
     * 计算优惠金额。
     *
     * 当前只支持固定金额满减券：
     *
     * 原始定金 13 元；
     * 优惠券减 5 元；
     * 实际支付 8 元。
     *
     * @param driverCouponDO 司机优惠券快照
     * @param originalAmount 原始定金金额
     * @return 优惠金额
     */
    private BigDecimal calculateCouponDiscount(ColdChainDriverCouponDO driverCouponDO, BigDecimal originalAmount) {

        BigDecimal discountAmount = driverCouponDO.getDiscountAmount();

        if (discountAmount == null || discountAmount.signum() <= 0) {
            throw new ColdChainBusinessException("优惠券金额异常");
        }

        /**
         * 当前暂不支持零元支付。
         *
         * 所以优惠金额必须小于原始定金。
         *
         * 如果以后支持零元支付，
         * 这里可以允许 discountAmount = originalAmount，
         * 然后直接走本地支付成功逻辑。
         */
        if (discountAmount.compareTo(originalAmount) >= 0) {
            throw new ColdChainBusinessException("优惠券金额异常，当前暂不支持零元付");
        }

        return discountAmount;
    }

    /**
     * 校验支付渠道回调金额。
     * BigDecimal 比较必须使用 compareTo。不要使用 equals。
     * 因为：
     * new BigDecimal("8.0").equals(new BigDecimal("8.00"))返回 false。
     * 但 compareTo 返回 0，表示数值相等。
     * @param payOrderDO 本地支付单
     * @param callbackPayAmount 支付渠道回调金额
     */
    private void validatePayAmount(ColdChainPayOrderDO payOrderDO, BigDecimal callbackPayAmount) {

        if (payOrderDO.getPayAmount() == null) {
            throw new ColdChainBusinessException("本地支付金额异常");
        }

        if (payOrderDO.getPayAmount().compareTo(callbackPayAmount) != 0) {
            throw new ColdChainBusinessException("支付回调金额与本地支付金额不一致");
        }
    }

    /**
     * 校验同一个 requestNo 是否被非法复用。
     *
     * @param payOrderDO 已存在支付单
     * @param driverId 当前司机 ID
     * @param orderId 当前订单 ID
     */
    private void validateExistingPayOrder(ColdChainPayOrderDO payOrderDO, Long driverId, Long orderId) {

        if (!Objects.equals(payOrderDO.getDriverId(), driverId)) {
            throw new ColdChainBusinessException("请求幂等号归属非法");
        }

        if (!Objects.equals(payOrderDO.getOrderId(), orderId)) {
            throw new ColdChainBusinessException("请求幂等号与订单不匹配");
        }
    }

    /**
     * 将数据库支付单转换成前端返回对象。
     *
     * @param payOrderDO 支付单
     * @return 创建支付单返回对象
     */
    private ColdChainCreatePayOrderVO buildCreatePayOrderVO(ColdChainPayOrderDO payOrderDO) {

        return new ColdChainCreatePayOrderVO(
                payOrderDO.getId(),
                payOrderDO.getPayOrderNo(),
                payOrderDO.getOriginalAmount(),
                payOrderDO.getCouponDiscountAmount(),
                payOrderDO.getPayAmount(),
                payOrderDO.getExpireTime(),
                payOrderDO.getPayStatus()
        );
    }

    /**
     * 生成支付单号。
     *
     * Java 侧保证随机性，
     * 数据库 uk_pay_order_no 再做最终唯一性兜底。
     *
     * @return 对外支付单号
     */
    private String generatePayOrderNo() {

        String timePart =
                LocalDateTime.now()
                        .format(PAY_ORDER_TIME_FORMATTER);

        String randomPart =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase();

        return "CCP" + timePart + randomPart;
    }

    /**
     * 判断字符串是否为空或只包含空格。
     *
     * @param value 待判断字符串
     * @return true：为空白字符串
     */
    private boolean isBlank(String value) {

        return StringUtils.isBlank(value) || StringUtils.isBlank(value.trim());
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
        queryWrapper.le(ColdChainPayOrderDO::getExpireTime, currentTime);

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
        updateWrapper.le(ColdChainPayOrderDO::getExpireTime, currentTime);
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
        queryWrapper.likeRight(org.springframework.util.StringUtils.hasText(payOrderNo), ColdChainPayOrderDO::getPayOrderNo, payOrderNo);

        /**
         * 只查询司机端真正需要的字段。
         *
         * 不返回未来可能存在的：
         * 渠道流水号、验签信息、内部风控字段等敏感信息。
         */
        queryWrapper.select(
                ColdChainPayOrderDO::getId,
                ColdChainPayOrderDO::getPayOrderNo,
                ColdChainPayOrderDO::getOrderId,
                ColdChainPayOrderDO::getId,
                ColdChainPayOrderDO::getPayAmount,
                ColdChainPayOrderDO::getPayStatus,
                ColdChainPayOrderDO::getExpireTime,
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
        pageVO.setBusinessOrderId(payOrderDO.getOrderId());
        pageVO.setCargoId(payOrderDO.getId());
        pageVO.setPayAmount(payOrderDO.getPayAmount());
        pageVO.setPayStatus(payOrderDO.getPayStatus());
        pageVO.setPayExpireTime(payOrderDO.getExpireTime());
        pageVO.setPaidTime(payOrderDO.getPaidTime());
        pageVO.setCreateTime(payOrderDO.getCreateTime());

        /**
         * 将状态编码转换为前端可展示文本。
         */
        pageVO.setPayStatusDesc(ColdChainPayStatusEnum.getDescByCode(payOrderDO.getPayStatus()));

        return pageVO;
    }
}
