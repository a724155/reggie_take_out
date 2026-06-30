package com.itheima.reggie.mapper;

import com.itheima.reggie.entity.ColdChainPayOrderDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 冷运支付单 Mapper。
 * <p>
 * 负责操作：
 * cold_chain_pay_order
 */
public interface IColdChainPayOrderMapper {

    /**
     * 根据前端请求幂等号查询支付单。
     * <p>
     * 使用场景：
     * <p>
     * 前端第一次点击“去支付”后网络超时，
     * 但后端其实已经成功创建了支付单。
     * <p>
     * 前端使用同一个 requestNo 重试时，
     * 后端直接返回原支付单，而不是再创建一笔。
     *
     * @param requestNo 前端请求幂等号
     * @return 已存在支付单；不存在时返回 null
     */
    ColdChainPayOrderDO selectByRequestNo(@Param("requestNo") String requestNo);

    /**
     * 根据冷运订单 ID 查询支付单。
     * <p>
     * 当前练习约束：
     * 一个冷运订单只能创建一笔支付单。
     * <p>
     * 数据库中有：
     * <p>
     * uk_order_id(order_id)
     * <p>
     * 这个唯一索引是最终防重保障。
     *
     * @param orderId 冷运订单 ID
     * @return 支付单；不存在时返回 null
     */
    ColdChainPayOrderDO selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据支付单主键查询支付单并加锁。
     * <p>
     * 当前主要给支付超时关闭逻辑使用。
     *
     * @param payOrderId 支付单 ID
     * @return 支付单；不存在时返回 null
     */
    ColdChainPayOrderDO selectByIdForUpdate(@Param("payOrderId") Long payOrderId);

    /**
     * 根据对外支付单号查询支付单并加锁。
     * <p>
     * 第三方支付渠道回调时，一般会带商户支付单号。
     * 真正业务中应优先使用 payOrderNo，而不是数据库 id。
     *
     * @param payOrderNo 对外支付单号
     * @return 支付单；不存在时返回 null
     */
    ColdChainPayOrderDO selectByPayOrderNoForUpdate(@Param("payOrderNo") String payOrderNo);

    /**
     * 新增支付单。
     * <p>
     * 数据库中有三个关键唯一索引：
     * <p>
     * uk_pay_order_no
     * uk_request_no
     * uk_order_id
     * <p>
     * 三层分别保证：
     * <p>
     * 1. 对外支付单号不重复；
     * 2. 同一个前端幂等请求不重复；
     * 3. 同一个冷运订单不重复创建支付单。
     *
     * @param payOrderDO 支付单对象
     * @return 受影响行数，正常应为 1
     */
    int insertPayOrder(ColdChainPayOrderDO payOrderDO);

    /**
     * 将待支付单更新为已支付。
     * <p>
     * 核心幂等条件：
     * <p>
     * pay_status = 10
     * <p>
     * 第一次支付回调：
     * <p>
     * WAIT_PAY -> PAID，更新成功，返回 1。
     * <p>
     * 后续重复支付回调：
     * <p>
     * 已经不是 WAIT_PAY，更新失败，返回 0。
     *
     * @param payOrderId     支付单 ID
     * @param channelTradeNo 三方渠道流水号
     * @param paidTime       渠道实际支付成功时间
     * @return 1：更新成功；0：状态已变化
     */
    int markPaySuccessIfWaiting(
            @Param("payOrderId") Long payOrderId,
            @Param("channelTradeNo") String channelTradeNo,
            @Param("paidTime") LocalDateTime paidTime
    );

    /**
     * 关闭已过期且仍未支付的支付单。
     * <p>
     * 注意：
     * 调用这个方法前，真实项目必须先查询支付渠道最终状态。
     * <p>
     * 不允许单纯因为本地时间过期，就直接关闭支付单。
     * <p>
     * 否则可能出现：
     * <p>
     * 司机在第 9 分 59 秒支付成功；
     * 支付回调还没到；
     * XXL-JOB 在第 10 分钟先把支付单关闭；
     * 然后错误释放了优惠券。
     *
     * @param payOrderId 支付单 ID
     * @param closedTime 支付单关闭时间
     * @return 1：关闭成功；0：状态已变化或尚未过期
     */
    int closePayOrderIfExpired(
            @Param("payOrderId") Long payOrderId,
            @Param("closedTime") LocalDateTime closedTime
    );
}
