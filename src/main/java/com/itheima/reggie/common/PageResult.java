package com.itheima.reggie.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果。
 *
 * @param <T> 当前页列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /**
     * 当前页数据列表。
     */
    private List<T> list;

    /**
     * 总记录数。
     */
    private Long total;

    /**
     * 当前页码，从 1 开始。
     */
    private Integer pageNo;

    /**
     * 每页条数。
     */
    private Integer pageSize;

    /**
     * 构建空分页结果。
     *
     * @param pageNo 当前页码
     * @param pageSize 每页条数
     * @param <T> 列表元素类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(Integer pageNo, Integer pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNo, pageSize);
    }
}
