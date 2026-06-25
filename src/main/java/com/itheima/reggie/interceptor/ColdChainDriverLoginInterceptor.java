package com.itheima.reggie.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 冷运司机登录拦截器。
 *
 * 作用：
 * 1. 在 Controller 执行前拦截司机端请求；
 * 2. 从请求中获取当前登录司机身份；
 * 3. 将司机 ID 写入 request attribute；
 * 4. Controller 后续通过 @RequestAttribute 获取当前司机 ID。
 *
 * 当前代码是 Controller 学习演示版：
 * 暂时从请求头 X-Driver-Id 中读取司机 ID。
 *
 * 真实企业项目中：
 * 通常会从 Authorization Token、Session、Redis 登录态、
 * 网关透传的用户信息中解析 driverId，
 * 不允许前端直接传真实 driverId。
 */
@Slf4j
@Component
public class ColdChainDriverLoginInterceptor implements HandlerInterceptor {

    /**
     * 请求属性名称。
     *
     * Controller 中的：
     * @RequestAttribute("currentDriverId")
     *
     * 必须与这里完全一致。
     */
    public static final String CURRENT_DRIVER_ID_ATTRIBUTE = "currentDriverId";

    /**
     * 在请求进入 Controller 之前执行。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前请求处理器
     * @return true：继续执行 Controller；false：拦截请求，不进入 Controller
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        /**
         * 当前仅用于演示：
         *
         * 前端请求头暂时传：
         * X-Driver-Id: 10001
         *
         * 真实项目不能相信这个值，
         * 必须通过 Token 解析出当前登录司机。
         */
        String driverIdText = request.getHeader("X-Driver-Id");

        if (StringUtils.isBlank(driverIdText)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long driverId;
        try {
            driverId = Long.valueOf(driverIdText);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        if (driverId <= 0L) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        /**
         * 将当前登录司机 ID 放入本次请求对象。
         *
         * 注意：
         * 这个值只在当前 HTTP 请求生命周期内有效。
         * 请求结束后自然消失，不是全局变量，也不是数据库字段。
         */
        request.setAttribute(CURRENT_DRIVER_ID_ATTRIBUTE, driverId);

        return true;
    }
}
