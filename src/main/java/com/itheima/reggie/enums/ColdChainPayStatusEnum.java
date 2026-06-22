package com.itheima.reggie.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ColdChainPayStatusEnum {

    /**
     * 待支付。
     *
     * 司机已经进入支付链路，但支付渠道还没有确认支付成功。
     */
    WAIT_PAY(10, "待支付"),

    /**
     * 已支付。
     *
     * 支付渠道回调验签、金额校验等全部通过后，支付单进入该状态。
     */
    PAID(20, "已支付"),

    /**
     * 已关闭。
     *
     * 常见原因：
     * 1. 超时未支付；
     * 2. 司机主动取消；
     * 3. 风控关闭；
     * 4. 支付渠道确认关闭。
     */
    CLOSED(30, "已关闭");

    /**
     * 数据库存储编码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 判断状态编码是否合法
     *
     * @param code 状态编码
     * @return true：合法；false：非法
     */
    public static boolean isValidCode(Integer code) {
        if (code == null) {
            return false;
        }

        return Arrays.stream(values())
                .anyMatch(status -> status.getCode().equals(code));
    }

    /**
     * 根据状态编码获取描述
     *
     * @param code 状态编码
     * @return 状态描述；不存在时返回空字符串
     */
    public static String getDescByCode(Integer code) {
        if (code == null) {
            return "";
        }

        return Arrays.stream(values())
                .filter(status -> status.getCode().equals(code))
                .map(ColdChainPayStatusEnum::getDesc)
                .findFirst()
                .orElse("");
    }
}
