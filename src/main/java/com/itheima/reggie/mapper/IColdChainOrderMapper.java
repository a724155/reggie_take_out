package com.itheima.reggie.mapper;

import com.itheima.reggie.entity.ColdChainOrderDO;
import org.apache.ibatis.annotations.Param;


/**
 * 冷运订单 Mapper。
 *
 * 负责操作：
 * cold_chain_order
 */
public interface IColdChainOrderMapper {

    /**
     * 根据订单 ID 查询订单，并对订单行加排他锁。
     *
     * 使用场景：
     * 创建支付单时锁定订单。
     *
     * 为什么需要 FOR UPDATE？
     *
     * 假设司机连续点击两次“去支付”：
     *
     * 请求 A：订单 1 + 优惠券 101；
     * 请求 B：订单 1 + 优惠券 102。
     *
     * 不锁订单时，两个请求可能同时创建两笔支付单，
     * 还可能锁住两张不同优惠券。
     *
     * 加锁后：
     *
     * 请求 A 先获得订单锁；
     * 请求 B 等待；
     * 请求 A 创建支付单并提交；
     * 请求 B 再获得锁后，发现订单已经存在支付单；
     * 请求 B 直接返回已有支付单，不会继续锁第二张券。
     *
     * 注意：
     * FOR UPDATE 必须在 @Transactional 方法中使用才有意义。
     *
     * @param orderId 冷运订单 ID
     * @return 冷运订单；不存在时返回 null
     */
    ColdChainOrderDO selectByIdForUpdate(@Param("orderId") Long orderId);

    /**
     * 条件更新订单状态。
     *
     * 例如：
     *
     * WAIT_DEPOSIT_PAY -> DEPOSIT_PAID
     *
     * 或：
     *
     * WAIT_DEPOSIT_PAY -> PAY_TIMEOUT
     *
     * 条件更新可以避免订单状态被重复推进。
     *
     * @param orderId 冷运订单 ID
     * @param driverId 当前司机 ID，防止水平越权
     * @param sourceOrderStatus 期待的原订单状态
     * @param targetOrderStatus 目标订单状态
     * @return 1：状态更新成功；0：订单不存在、归属不匹配或状态已变化
     */
    int updateOrderStatus(
            @Param("orderId") Long orderId,
            @Param("driverId") Long driverId,
            @Param("sourceOrderStatus") Integer sourceOrderStatus,
            @Param("targetOrderStatus") Integer targetOrderStatus
    );
}
