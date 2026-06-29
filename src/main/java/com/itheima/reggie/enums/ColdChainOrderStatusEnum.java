package com.itheima.reggie.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 冷运订单支付阶段状态。
 *
 * 这里只列出本优惠券需求需要使用的状态。
 */
@Getter
@AllArgsConstructor
public enum ColdChainOrderStatusEnum {

    /**
     * 已创建订单，等待支付定金。
     */
    WAIT_DEPOSIT_PAY(10, "等待支付定金"),

    /**
     * 定金支付成功。
     */
    DEPOSIT_PAID(20, "定金已支付"),

    /**
     * 支付超时。
     */
    PAY_TIMEOUT(30, "支付超时");

    private final Integer code;

    private final String desc;
}
