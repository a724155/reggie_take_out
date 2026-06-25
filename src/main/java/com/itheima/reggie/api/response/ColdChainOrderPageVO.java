package com.itheima.reggie.api.response;

import lombok.Data;

/**
 * 冷运司机端订单分页展示对象。
 */
@Data
public class ColdChainOrderPageVO {

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
     * 应付金额，单位：分。
     */
    private Long payAmount;
}
