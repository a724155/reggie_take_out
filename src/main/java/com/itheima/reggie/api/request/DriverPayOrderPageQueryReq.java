package com.itheima.reggie.api.request;


import lombok.Data;

/**
 * 司机端支付单分页查询请求
 */
@Data
public class DriverPayOrderPageQueryReq {

    /**
     * 页码。
     *
     * 从 1 开始。
     */
    private Long pageNo = 1L;

    /**
     * 每页条数。
     *
     * 建议控制最大值，防止前端一次拉取过多数据。
     */
    private Long pageSize = 20L;

    /**
     * 支付状态。
     *
     * 可选：
     * 10：待支付
     * 20：已支付
     * 30：已关闭
     */
    private Integer payStatus;

    /**
     * 支付单号前缀。
     *
     * 用于模糊搜索。
     */
    private String payOrderNo;
}
