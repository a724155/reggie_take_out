package com.itheima.reggie.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 冷运司机订单分页列表展示对象。
 *
 * 该对象只表达前端页面需要展示的数据，
 * 不应该直接把数据库实体 DO 原样返回给前端。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColdChainDriverOrderPageVO {

    /**
     * 冷运订单 ID。
     */
    private Long orderId;

    /**
     * 货物名称。
     */
    private String cargoName;

    /**
     * 起始地。
     */
    private String startAddress;

    /**
     * 目的地。
     */
    private String endAddress;

    /**
     * 定金金额。
     */
    private BigDecimal depositAmount;

    /**
     * 订单状态编码。
     */
    private Integer orderStatus;

    /**
     * 订单状态描述。
     */
    private String orderStatusDesc;
}
