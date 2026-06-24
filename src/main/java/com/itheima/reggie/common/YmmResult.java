package com.itheima.reggie.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 通用接口返回结果。
 *
 * @param <T> data 字段的数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YmmResult<T> {

    /**
     * 成功编码。
     */
    private static final Integer SUCCESS_CODE = 1;

    /**
     * 默认失败编码。
     */
    private static final Integer FAIL_CODE = 0;

    /**
     * 业务响应编码。
     *
     * 1：成功；
     * 0 或其他编码：失败。
     */
    private Integer code;

    /**
     * 响应提示信息。
     */
    private String msg;

    /**
     * 业务响应数据。
     */
    private T data;

    /**
     * 构建成功响应结果。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应结果
     */
    public static <T> YmmResult<T> success(T data) {
        return new YmmResult<>(SUCCESS_CODE, null, data);
    }

    /**
     * 构建无业务数据的成功响应结果。
     *
     * @return 成功响应结果
     */
    public static YmmResult<Void> success() {
        return YmmResult.<Void>success(null);
    }

    /**
     * 构建失败响应结果。
     *
     * @param code 失败编码
     * @param msg 失败提示信息
     * @param <T> 业务数据类型
     * @return 失败响应结果
     */
    public static <T> YmmResult<T> fail(Integer code, String msg) {
        return new YmmResult<>(code, msg, null);
    }

    /**
     * 构建使用默认失败编码的失败响应结果。
     *
     * @param msg 失败提示信息
     * @param <T> 业务数据类型
     * @return 失败响应结果
     */
    public static <T> YmmResult<T> fail(String msg) {
        return fail(FAIL_CODE, msg);
    }
}
