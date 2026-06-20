package com.itheima.reggie.common;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用返回结果，服务端响应的数据最终都会封装成此对象
 * @param <T>
 */
@Data
public class ReggieResult<T> {

    /**
     * 编码：1成功，0和其它数字为失败
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String msg;

    /**
     * 数据
     */
    private T data;

    /**
     * 动态数据
     */
    private Map map = new HashMap();

    public static <T> ReggieResult<T> success(T object) {
        ReggieResult<T> reggieResult = new ReggieResult<T>();
        reggieResult.data = object;
        reggieResult.code = 1;
        return reggieResult;
    }

    public static <T> ReggieResult<T> error(String msg) {
        ReggieResult reggieResult = new ReggieResult();
        reggieResult.msg = msg;
        reggieResult.code = 0;
        return reggieResult;
    }

    public ReggieResult<T> add(String key, Object value) {
        this.map.put(key, value);
        return this;
    }

}
