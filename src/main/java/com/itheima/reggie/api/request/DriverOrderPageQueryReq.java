package com.itheima.reggie.api.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 冷运司机端订单分页查询条件。
 */
@Data
public class DriverOrderPageQueryReq {

    /**
     * 页码，从 1 开始。
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNo;

    /**
     * 每页条数。
     */
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize;

    /**
     * 订单状态，可不传。
     *
     * 例如：
     * 10：待支付；
     * 20：已支付；
     * 30：已关闭。
     */
    private Integer orderStatus;

    /**
     * 订单号，支持按订单号筛选，可不传。
     */
    private String orderNo;
}
