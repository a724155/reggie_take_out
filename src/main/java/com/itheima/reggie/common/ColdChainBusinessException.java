package com.itheima.reggie.common;

import lombok.Getter;

/**
 * 冷运业务异常。
 *
 * 用于表示“业务规则不满足”，例如：
 * 1. 优惠券已领完；
 * 2. 司机重复领券；
 * 3. 优惠券已锁定；
 * 4. 无权使用该优惠券；
 * 5. 订单状态不允许创建支付单。
 *
 * 注意：
 * 该异常会被全局异常处理器捕获，
 * 最终转换成统一 JSON 响应返回给前端，
 * 不会导致整个应用程序停止运行。
 */
@Getter
public class ColdChainBusinessException extends RuntimeException {

    /**
     * 面向前端的业务错误码。
     *
     * 不建议所有失败都只返回 code = 0。
     * 后面前端、埋点、监控、运营分析时，
     * 可以根据不同 code 区分具体失败原因。
     */
    private final Integer code;

    /**
     * 创建冷运业务异常。
     *
     * @param code 业务错误码
     * @param message 给前端展示的提示信息
     */
    public ColdChainBusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用默认冷运业务错误码创建异常。
     *
     * @param message 给前端展示的提示信息
     */
    public ColdChainBusinessException(String message) {
        this(40000, message);
    }
}
