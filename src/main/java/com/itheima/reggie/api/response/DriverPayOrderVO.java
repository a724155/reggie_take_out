package com.itheima.reggie.api.response;

import lombok.Data;

/**
 * 冷运司机端支付单展示对象。
 */
@Data
public class DriverPayOrderVO {

    /**
     * 支付单号。
     */
    private String payOrderNo;

    /**
     * 关联冷运订单 ID。
     */
    private Long orderId;

    /**
     * 支付金额，单位：分。
     */
    private Long payAmount;

    /**
     * 支付状态编码。
     */
    private Integer payStatus;

    /**
     * 支付状态描述。
     */
    private String payStatusDesc;

    /**
     * 第三方支付流水号。
     */
    private String thirdPayOrderNo;
}