package com.itheima.reggie.dto;

import lombok.Data;

/**
 * 冷运司机订单分页查询请求。
 *
 * Controller 会将 URL 中的查询参数自动绑定到该对象。
 *
 * 例如：
 * /cold-chain/driver/orders?pageNo=1&pageSize=10&orderStatus=10
 */
@Data
public class ColdChainDriverOrderPageQueryReq {

    /**
     * 页码，从 1 开始。
     */
    private Integer pageNo = 1;

    /**
     * 每页条数。
     */
    private Integer pageSize = 10;

    /**
     * 订单状态。
     *
     * 当前先使用 Integer 模拟。
     * 后面接入枚举后会改成 ColdChainOrderStatusEnum。
     */
    private Integer orderStatus;
}
