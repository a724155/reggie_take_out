package com.itheima.reggie.mapper;

import com.itheima.reggie.entity.ColdChainDriverCouponDO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 冷运司机优惠券 Mapper。
 *
 * 负责操作：
 * cold_chain_driver_coupon
 *
 * 这张表是真正记录“司机拥有的优惠券”的表。
 *
 * 优惠券模板只是运营规则；
 * 司机优惠券才是司机真正可以使用的权益。
 */
public interface IColdChainDriverCouponMapper {

    /**
     * 根据司机优惠券 ID 查询优惠券。
     *
     * 当前主要用于：
     * 创建支付单时读取优惠券金额快照、使用门槛、归属司机和状态。
     *
     * @param driverCouponId 司机优惠券 ID
     * @return 司机优惠券；不存在时返回 null
     */
    ColdChainDriverCouponDO selectById(@Param("driverCouponId") Long driverCouponId);

    /**
     * 查询司机是否已经领取过某个优惠券模板。
     *
     * 这一步用于给前端返回友好提示。
     *
     * 但它不是最终防重保障。
     * 最终防重仍然依赖数据库唯一索引：
     *
     * uk_template_driver(template_id, driver_id)
     *
     * @param templateId 优惠券模板 ID
     * @param driverId 司机 ID
     * @return 已领取的司机优惠券；没有领取过时返回 null
     */
    ColdChainDriverCouponDO selectByTemplateIdAndDriverId(
            @Param("templateId") Long templateId,
            @Param("driverId") Long driverId);

    /**
     * 新增司机优惠券。
     *
     * 领取优惠券时：
     * 先扣减模板库存，再插入司机优惠券。
     *
     * 两步必须放在同一个事务里。
     *
     * @param driverCouponDO 司机优惠券对象
     * @return 受影响行数，正常应为 1
     */
    int insertDriverCoupon(ColdChainDriverCouponDO driverCouponDO);

    /**
     * 锁定司机优惠券。
     *
     * 创建支付单时执行。
     *
     * 核心目标：
     * 防止同一张优惠券被两个订单同时使用。
     *
     * SQL 中最关键的条件：
     *
     * coupon_status = 10
     *
     * 即优惠券必须处于“未使用”状态。
     *
     * 多个请求同时锁同一张券时：
     *
     * 第一个请求更新成功，返回 1；
     * 后续请求更新失败，返回 0。
     *
     * @param driverCouponId 司机优惠券 ID
     * @param driverId 当前司机 ID，防止使用他人优惠券
     * @param orderId 当前订单 ID
     * @param orderAmount 当前订单原始定金金额
     * @param lockExpireTime 锁券过期时间，通常与支付单过期时间一致
     * @param currentTime 当前时间
     * @return 1：锁券成功；0：优惠券不可用、过期、不属于当前司机或不满足门槛
     */
    int lockCouponForPay(
            @Param("driverCouponId") Long driverCouponId,
            @Param("driverId") Long driverId,
            @Param("orderId") Long orderId,
            @Param("orderAmount") BigDecimal orderAmount,
            @Param("lockExpireTime") LocalDateTime lockExpireTime,
            @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * 支付成功后核销优惠券。
     *
     * 只有满足下列条件才允许核销：
     *
     * 1. 优惠券属于当前付款司机；
     * 2. 优惠券状态必须是 LOCKED；
     * 3. 优惠券锁定的订单必须就是当前支付成功订单。
     *
     * @param driverCouponId 司机优惠券 ID
     * @param driverId 当前司机 ID
     * @param orderId 当前订单 ID
     * @param usedTime 优惠券核销时间
     * @return 1：核销成功；0：状态异常
     */
    int consumeLockedCoupon(
            @Param("driverCouponId") Long driverCouponId,
            @Param("driverId") Long driverId,
            @Param("orderId") Long orderId,
            @Param("usedTime") LocalDateTime usedTime
    );

    /**
     * 支付超时后释放优惠券。
     *
     * 注意：
     * 必须带 driverId 和 lockOrderId 条件。
     *
     * 否则可能出现这种严重问题：
     *
     * 订单 A 锁住了优惠券；
     * 后来订单 B 误把这张券释放；
     * 最终导致一张券被多个订单使用。
     *
     * @param driverCouponId 司机优惠券 ID
     * @param driverId 当前司机 ID
     * @param orderId 当前订单 ID
     * @return 1：释放成功；0：状态异常
     */
    int releaseLockedCoupon(
            @Param("driverCouponId") Long driverCouponId,
            @Param("driverId") Long driverId,
            @Param("orderId") Long orderId
    );
}
