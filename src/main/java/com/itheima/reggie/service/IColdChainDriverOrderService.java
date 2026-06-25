package com.itheima.reggie.service;

import com.itheima.reggie.api.request.DriverCancelOrderReq;
import com.itheima.reggie.api.request.DriverOrderPageQueryReq;
import com.itheima.reggie.api.request.DriverPaySubmitReq;
import com.itheima.reggie.api.response.ColdChainOrderDetailVO;
import com.itheima.reggie.api.response.ColdChainOrderPageVO;
import com.itheima.reggie.api.response.DriverPayOrderVO;
import com.itheima.reggie.api.response.PaySubmitVO;
import com.itheima.reggie.common.PageResult;

import java.util.List;

public interface IColdChainDriverOrderService {

    /**
     * 查询当前司机可见的冷运订单分页列表。
     *
     * @param driverId 当前登录司机 ID
     * @param request 分页查询条件
     * @return 订单分页结果
     */
    PageResult<ColdChainOrderPageVO> queryDriverOrderPage(Long driverId, DriverOrderPageQueryReq request);

    /**
     * 查询当前司机可见的订单详情。
     *
     * @param driverId 当前登录司机 ID
     * @param orderId 冷运订单 ID
     * @return 订单详情
     */
    ColdChainOrderDetailVO queryDriverOrderDetail(Long driverId, Long orderId);

    /**
     * 查询当前司机指定订单下的支付单列表。
     *
     * @param driverId 当前登录司机 ID
     * @param orderId 冷运订单 ID
     * @param includeClosed 是否包含已关闭支付单
     * @return 支付单列表
     */
    List<DriverPayOrderVO> queryDriverPayOrderList(Long driverId, Long orderId, Boolean includeClosed);

    /**
     * 当前司机提交支付申请。
     *
     * @param driverId 当前登录司机 ID
     * @param orderId 冷运订单 ID
     * @param request 支付提交参数
     * @param idempotencyKey 幂等键，用于防止司机重复点击支付
     * @return 收银台信息
     */
    PaySubmitVO submitDriverPay(Long driverId, Long orderId, DriverPaySubmitReq request, String idempotencyKey);

    /**
     * 当前司机取消自己的冷运订单。
     *
     * @param driverId 当前登录司机 ID
     * @param orderId 冷运订单 ID
     * @param request 取消订单请求
     */
    void cancelDriverOrder(Long driverId, Long orderId, DriverCancelOrderReq request);
}
