package com.itheima.reggie.api.response;

import lombok.Data;

/**
 * 冷运司机端订单详情展示对象。
 */
@Data
public class ColdChainOrderDetailVO {

    /**
     * 冷运订单 ID。
     */
    private Long orderId;

    /**
     * 冷运订单号。
     */
    private String orderNo;

    /**
     * 货物名称。
     */
    private String cargoName;

    /**
     * 货物重量，单位：千克。
     */
    private Long cargoWeight;

    /**
     * 装货地址。
     */
    private String loadAddress;

    /**
     * 卸货地址。
     */
    private String unloadAddress;

    /**
     * 订单状态编码。
     */
    private Integer orderStatus;

    /**
     * 订单状态描述。
     */
    private String orderStatusDesc;

    /**
     * 支付单号。
     */
    private String payOrderNo;

    /**
     * 支付状态编码。
     */
    private Integer payStatus;

    /**
     * 支付状态描述。
     */
    private String payStatusDesc;

    /**
     * 应付金额，单位：分。
     */
    private Long payAmount;
}
