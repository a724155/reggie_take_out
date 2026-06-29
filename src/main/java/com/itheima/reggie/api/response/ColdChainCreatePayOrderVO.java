package com.itheima.reggie.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建冷运支付单返回对象。
 *
 * 前端拿到该对象后：
 * 1. 展示优惠金额；
 * 2. 展示实际支付金额；
 * 3. 根据 payOrderNo 拉起支付渠道；
 * 4. 根据 expireTime 展示倒计时。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColdChainCreatePayOrderVO {

    /**
     * 本地支付单主键。
     */
    private Long payOrderId;

    /**
     * 对外支付单号。
     */
    private String payOrderNo;

    /**
     * 原始定金金额。
     */
    private BigDecimal originalAmount;

    /**
     * 本次优惠金额。
     */
    private BigDecimal couponDiscountAmount;

    /**
     * 最终实际支付金额。
     */
    private BigDecimal payAmount;

    /**
     * 支付超时时间。
     */
    private LocalDateTime expireTime;

    /**
     * 支付单状态。
     */
    private Integer payStatus;
}
