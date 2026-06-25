package com.itheima.reggie.api.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 冷运司机取消订单请求。
 */
@Data
public class DriverCancelOrderReq {

    /**
     * 取消原因。
     */
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 200, message = "取消原因不能超过200个字符")
    private String cancelReason;
}