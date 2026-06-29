package com.itheima.reggie.api.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * 创建冷运支付单请求。
 *
 * 前端只允许提交：
 * 1. 司机选中的优惠券实例 ID；
 * 2. 本次点击支付的幂等号。
 *
 * 原始金额、优惠金额、实际支付金额全部由服务端计算。
 */
@Data
public class ColdChainCreatePayOrderReq {

    /**
     * 前端幂等号。
     *
     * 前端第一次点击“去支付”时生成 UUID。
     * 网络超时重试时必须复用同一个 requestNo。
     *
     * 数据库对 request_no 建唯一索引，
     * 防止重复点击或网络重试重复创建支付单。
     */
    @NotBlank(message = "请求幂等号不能为空")
    @Size(max = 64, message = "请求幂等号长度不能超过64位")
    private String requestNo;

    /**
     * 司机选择的“我的优惠券 ID”。
     *
     * 允许为空。
     * 为空表示本次支付不使用优惠券。
     *
     * 注意：
     * 这里不是优惠券模板 ID，而是司机已经领取到自己账户下的优惠券 ID。
     */
    @Positive(message = "司机优惠券ID必须大于0")
    private Long driverCouponId;
}