package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.extension.api.R;
import com.itheima.reggie.api.request.ColdChainCreatePayOrderReq;
import com.itheima.reggie.api.request.DriverPayOrderPageQueryReq;
import com.itheima.reggie.api.response.ColdChainCreatePayOrderVO;
import com.itheima.reggie.api.response.DriverPayOrderPageVO;
import com.itheima.reggie.common.ColdChainRequestAttributeConstants;
import com.itheima.reggie.common.OrderPageResult;
import com.itheima.reggie.common.YmmResult;
import com.itheima.reggie.service.IColdChainPayOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

/**
 * 冷运司机端支付单接口
 */
@RestController
@RequestMapping("/cold-chain/driver/orders")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainDriverPayOrderController {

    /**
     * 冷运支付单服务
     */
    private final IColdChainPayOrderService coldChainPayOrderService;

    /**
     * 查询当前司机支付单分页列表
     * <p>
     * 请求示例：
     * <p>
     * GET /cold-chain/driver/orders?pageNo=1&pageSize=20&payStatus=10
     *
     * @param driverId 当前登录司机ID，由认证层注入
     * @param request  分页查询条件
     * @return 支付单分页列表
     */
    @GetMapping
    public OrderPageResult<DriverPayOrderPageVO> queryDriverPayOrderPage(@RequestAttribute("currentDriverId") Long driverId, @ModelAttribute DriverPayOrderPageQueryReq request) {
        return coldChainPayOrderService.queryDriverPayOrderPage(driverId, request);
    }

    /**
     * 创建冷运订单支付单。
     * 请求示例：
     * POST /cold-chain/driver/orders/1/pay-orders
     * 请求体：
     * {
     *     "requestNo": "coupon-demo-driver-10001-001",
     *     "driverCouponId": 1
     * }
     * @param driverId 当前登录司机 ID，由拦截器注入
     * @param orderId 冷运订单 ID
     * @param request 创建支付单请求
     * @return 支付单信息
     */
    @PostMapping("/{orderId}/pay-orders")
    public YmmResult<ColdChainCreatePayOrderVO> createPayOrder(@RequestAttribute(ColdChainRequestAttributeConstants.CURRENT_DRIVER_ID) Long driverId,
            @PathVariable @Positive(message = "订单ID必须大于0") Long orderId,
            @Valid @RequestBody ColdChainCreatePayOrderReq request) {

        ColdChainCreatePayOrderVO payOrderVO = coldChainPayOrderService.createPayOrder(driverId, orderId, request);
        return YmmResult.success(payOrderVO);
    }


}
