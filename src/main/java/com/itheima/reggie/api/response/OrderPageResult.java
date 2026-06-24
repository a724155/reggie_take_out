package com.itheima.reggie.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应对象
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPageResult<T> {

    /**
     * 当前页码
     */
    private Long pageNo;

    /**
     * 每页条数
     */
    private Long pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页数据
     */
    private List<T> recordList;

    /**
     * 创建空分页结果
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param <T> 泛型类型
     * @return 空分页结果
     */
    public static <T> OrderPageResult<T> empty(Long pageNo, Long pageSize) {
        return new OrderPageResult<>(
                pageNo,
                pageSize,
                0L,
                Collections.emptyList()
        );
    }
}
