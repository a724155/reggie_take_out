package com.itheima.reggie.api.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 冷运司机提交支付请求。
 */
@Data
public class DriverPaySubmitReq {

    /**
     * 支付渠道。
     *
     * 例如：
     * ALIPAY：支付宝；
     * WECHAT：微信支付。
     */
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    /**
     * 优惠券 ID，可不传。
     */
    private Long couponId;
}
