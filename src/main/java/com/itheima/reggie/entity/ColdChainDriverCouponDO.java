package com.itheima.reggie.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 司机实际拥有的优惠券。
 *
 * 司机使用优惠券时，查询的是这张表，
 * 而不是直接查询优惠券模板。
 */
@Data
public class ColdChainDriverCouponDO {

    /**
     * 司机优惠券 ID。
     */
    private Long id;

    /**
     * 来源优惠券模板 ID。
     */
    private Long templateId;

    /**
     * 归属司机 ID。
     */
    private Long driverId;

    /**
     * 优惠券名称快照。
     */
    private String couponName;

    /**
     * 优惠金额快照。
     */
    private BigDecimal discountAmount;

    /**
     * 使用门槛快照。
     */
    private BigDecimal thresholdAmount;

    /**
     * 券有效开始时间。
     */
    private LocalDateTime validStartTime;

    /**
     * 券有效结束时间。
     */
    private LocalDateTime validEndTime;

    /**
     * 优惠券状态。
     *
     * 10：未使用
     * 20：锁定
     * 30：已使用
     * 40：已过期
     */
    private Integer couponStatus;

    /**
     * 当前锁定的订单 ID。
     *
     * 只有 LOCKED 状态下才有值。
     */
    private Long lockOrderId;

    /**
     * 锁券超时时间。
     */
    private LocalDateTime lockExpireTime;

    /**
     * 最终使用在哪个订单。
     */
    private Long usedOrderId;

    /**
     * 最终使用时间。
     */
    private LocalDateTime usedTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
