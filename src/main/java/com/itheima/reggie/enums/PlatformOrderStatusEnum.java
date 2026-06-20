package com.itheima.reggie.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 平台订单状态。
 */
@Getter
@AllArgsConstructor
public enum PlatformOrderStatusEnum {

    /**
     * 待支付。
     */
    WAIT_PAY(1, "待支付"),

    /**
     * 已支付，等待商家接单。
     */
    PAID(2, "已支付"),

    /**
     * 商家已接单。
     */
    ACCEPTED(3, "已接单"),

    /**
     * 配送中。
     */
    DELIVERING(4, "配送中"),

    /**
     * 已完成。
     */
    FINISHED(5, "已完成"),

    /**
     * 已取消。
     */
    CANCELED(6, "已取消"),

    /**
     * 已退款。
     */
    REFUNDED(7, "已退款");

    private final Integer code;

    private final String desc;

    /**
     * 根据状态编码获取枚举。
     *
     * @param code 状态编码
     * @return 对应状态枚举；不存在时返回 null
     */
    public static PlatformOrderStatusEnum getByCode(Integer code) {
        if (Objects.isNull(code)) {
            return null;
        }

        for (PlatformOrderStatusEnum statusEnum : values()) {
            if (Objects.equals(statusEnum.getCode(), code)) {
                return statusEnum;
            }
        }

        return null;
    }
}
