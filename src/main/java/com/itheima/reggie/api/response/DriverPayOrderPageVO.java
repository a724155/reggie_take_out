package com.itheima.reggie.api.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 司机端支付单分页列表返回对象
 */
@Data
public class DriverPayOrderPageVO {

    /**
     * 支付单ID
     */
    private Long payOrderId;

    /**
     * 支付单号
     */
    private String payOrderNo;

    /**
     * 业务订单ID
     */
    private Long businessOrderId;

    /**
     * 货源ID
     */
    private Long cargoId;

    /**
     * 定金金额
     */
    private BigDecimal payAmount;

    /**
     * 支付状态编码
     */
    private Integer payStatus;

    /**
     * 支付状态描述
     */
    private String payStatusDesc;

    /**
     * 支付截止时间
     */
    private LocalDateTime payExpireTime;

    /**
     * 支付成功时间
     */
    private LocalDateTime paidTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
