package com.itheima.reggie.common;

/**
 * 冷运请求上下文属性常量。
 *
 * 用于统一管理 request.setAttribute() 和 @RequestAttribute()
 * 使用的 key，避免两边手写字符串时拼错。
 */
public final class ColdChainRequestAttributeConstants {

    /**
     * 当前请求所属司机 ID。
     *
     * 该值由拦截器写入 HttpServletRequest，
     * Controller 使用 @RequestAttribute 读取。
     */
    public static final String CURRENT_DRIVER_ID = "currentDriverId";

    /**
     * 工具类禁止被实例化。
     */
    private ColdChainRequestAttributeConstants() {
    }
}
