package com.itheima.reggie.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 冷运司机优惠券状态。
 *
 * 注意：
 * 优惠券状态不要使用字符串，例如 "未使用"、"已使用"。
 * 数据库存数字，代码用枚举解释数字，前端返回时再转换成中文描述。
 */
@Getter
@AllArgsConstructor
public enum ColdChainCouponStatusEnum {

    /**
     * 已领取但尚未使用。
     */
    UNUSED(10, "未使用"),

    /**
     * 已绑定到某个支付单，等待支付结果。
     */
    LOCKED(20, "已锁定"),

    /**
     * 支付成功后完成核销。
     */
    USED(30, "已使用"),

    /**
     * 超过优惠券有效期。
     */
    EXPIRED(40, "已过期");

    private final Integer code;

    private final String desc;
}
