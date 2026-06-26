package com.itheima.reggie.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

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
     * 练习阶段使用的司机 ID 请求头。
     *
     * 例如：
     * X-Driver-Id: 10001
     */
    private static final String DRIVER_ID_HEADER_NAME = "X-Driver-Id";

    /**
     * Controller 执行前进行司机身份校验。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前将要执行的 Controller 方法
     * @return true：继续进入 Controller；
     *         false：请求被拦截，不再进入 Controller

    /**
     * 在请求进入 Controller 之前执行。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler 当前请求处理器
     * @return true：继续执行 Controller；false：拦截请求，不进入 Controller
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        /**
         * 浏览器跨域时可能先发送 OPTIONS 预检请求。
         *
         * 当前先直接放行，后面学习跨域配置时再深入处理。
         */
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        /**
         * 当前仅用于演示：
         *
         * 前端请求头暂时传：
         * X-Driver-Id: 10001
         *
         * 真实项目不能相信这个值，
         * 必须通过 Token 解析出当前登录司机。
         */
        String driverIdText = request.getHeader(DRIVER_ID_HEADER_NAME);

        if (StringUtils.isBlank(driverIdText)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Long driverId = parsePositiveLong(driverIdText);
        /**
         * 没有司机身份，直接拦截请求。
         */
        if (Objects.isNull(driverId)) {
            log.warn("冷运司机接口访问被拒绝，请求路径：{}，原因：司机身份缺失或非法", request.getRequestURI());
            writeUnauthorizedResponse(response, "未登录、登录已过期或司机身份非法");
            return false;
        }


        /**
         * 将当前司机 ID 写入本次 HTTP 请求。
         *
         * 后续 Controller 可通过：
         * @RequestAttribute(CURRENT_DRIVER_ID)
         * 自动获取该值。
         */
        request.setAttribute(CURRENT_DRIVER_ID_ATTRIBUTE, driverId);
        log.info("冷运司机身份校验通过，driverId={}，requestUri={}", driverId, request.getRequestURI());
        return true;
    }

    /**
     * 将字符串解析为正数 Long。
     *
     * @param value 待解析字符串
     * @return 合法正数返回对应 Long；非法则返回 null
     */
    private Long parsePositiveLong(String value) {
        if (StringUtils.isBlank(value) || StringUtils.isEmpty(value.trim())) {
            return null;
        }
        try {
            Long id = Long.valueOf(value.trim());
            if (id <= 0) {
                return null;
            }
            return id;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 返回未登录响应。
     *
     * 当前为了让拦截器尽量独立，直接写 JSON。
     * 后面我们会再把这一层统一收敛到全局异常处理器。
     *
     * @param response HTTP 响应对象
     * @param message 错误信息
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\",\"data\":null}");
    }
}
