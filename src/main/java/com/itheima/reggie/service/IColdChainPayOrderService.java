package com.itheima.reggie.service;

import com.itheima.reggie.api.request.ColdChainCreatePayOrderReq;
import com.itheima.reggie.api.request.DriverPayOrderPageQueryReq;
import com.itheima.reggie.api.response.ColdChainCreatePayOrderVO;
import com.itheima.reggie.api.response.DriverPayOrderPageVO;
import com.itheima.reggie.common.OrderPageResult;
import com.itheima.reggie.entity.ColdChainPayOrderDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 冷运定金支付单服务接口
 */
public interface IColdChainPayOrderService {

    /**
     * 查询司机自己的支付单分页列表
     *
     * @param driverId 当前登录司机ID
     * @param request  分页查询条件
     * @return 支付单分页数据
     */
    OrderPageResult<DriverPayOrderPageVO> queryDriverPayOrderPage(Long driverId, DriverPayOrderPageQueryReq request);

    /**
     * 分批查询超时但仍处于待支付状态的支付单
     * <p>
     * 该方法供 XXL-JOB 调用。
     *
     * @param lastPayOrderId 上一批处理到的支付单ID
     * @param limit          单次最大查询数量
     * @param currentTime    当前时间
     * @return 超时支付单列表
     */
    List<ColdChainPayOrderDO> queryExpiredWaitPayOrderList(Long lastPayOrderId, Integer limit, LocalDateTime currentTime);

    /**
     * 超时关闭支付单
     * <p>
     * 只有待支付且已经超时的支付单才允许关闭。
     *
     * @param payOrderId  支付单ID
     * @param currentTime 当前时间
     * @return true：本次成功关闭；false：订单已被其他流程处理或不满足关闭条件
     */
    boolean closePayOrderIfWaitingAndExpired(Long payOrderId, LocalDateTime currentTime);

    /**
     * 支付渠道确认成功后，将支付单推进为已支付
     *
     * @param payOrderId 支付单ID
     * @param paidTime   支付成功时间
     * @return true：状态推进成功；false：支付单已不是待支付状态
     */
    boolean markPayOrderPaidIfWaiting(Long payOrderId, LocalDateTime paidTime);


    /**
     * 创建支付单。
     *
     * @param driverId 当前登录司机 ID
     * @param orderId 冷运订单 ID
     * @param request 创建支付单请求
     * @return 创建成功后的支付单信息
     */
    ColdChainCreatePayOrderVO createPayOrder(Long driverId, Long orderId, ColdChainCreatePayOrderReq request);

    /**
     * 处理支付渠道成功回调。
     *
     * 注意：
     * 调用本方法之前，必须先完成支付渠道验签。
     *
     * @param payOrderNo 商户支付单号
     * @param channelTradeNo 第三方支付渠道流水号
     * @param callbackPayAmount 支付渠道实际支付金额
     * @param channelPaidTime 支付渠道记录的实际成功时间
     */
    void handlePaySuccess(String payOrderNo, String channelTradeNo, BigDecimal callbackPayAmount, LocalDateTime channelPaidTime);

    /**
     * 支付超时关闭处理。
     *
     * 该方法的前提是：
     * 已经向支付渠道确认该笔支付最终未支付。
     *
     * @param payOrderId 支付单 ID
     */
    void closeExpiredPayOrderAfterChannelConfirmedUnpaid(Long payOrderId);








}
