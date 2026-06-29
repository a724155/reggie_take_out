package com.itheima.reggie.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 冷运支付单数据库实体。
 *
 * 对应表：
 * cold_chain_pay_order
 */
@Data
@TableName("cold_chain_pay_order")
public class ColdChainPayOrderDO {

    /**
     * 支付单主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对外支付单号。
     */
    private String payOrderNo;

    /**
     * 前端请求幂等号。
     */
    private String requestNo;

    /**
     * 冷运订单 ID。
     */
    private Long orderId;

    /**
     * 付款司机 ID。
     */
    private Long driverId;

    /**
     * 原始定金金额。
     */
    private BigDecimal originalAmount;

    /**
     * 使用的司机优惠券 ID。
     */
    private Long driverCouponId;

    /**
     * 优惠抵扣金额。
     */
    private BigDecimal couponDiscountAmount;

    /**
     * 实际支付金额。
     */
    private BigDecimal payAmount;

    /**
     * 支付状态：
     *
     * 10：待支付
     * 20：已支付
     * 30：已关闭
     */
    private Integer payStatus;

    /**
     * 支付过期时间。
     */
    private LocalDateTime expireTime;

    /**
     * 第三方支付渠道流水号。
     */
    private String channelTradeNo;

    /**
     * 支付成功时间。
     */
    private LocalDateTime paidTime;

    /**
     * 支付关闭时间。
     */
    private LocalDateTime closedTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
