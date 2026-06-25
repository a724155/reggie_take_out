package com.itheima.reggie.controller;


import com.itheima.reggie.api.request.DriverPayOrderPageQueryReq;
import com.itheima.reggie.api.response.DriverPayOrderPageVO;
import com.itheima.reggie.common.OrderPageResult;
import com.itheima.reggie.service.IColdChainPayOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 冷运司机端支付单接口
 */
@RestController
@RequestMapping("/cold-chain/driver/pay-orders")
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
     * GET /cold-chain/driver/pay-orders?pageNo=1&pageSize=20&payStatus=10
     *
     * @param driverId 当前登录司机ID，由认证层注入
     * @param request  分页查询条件
     * @return 支付单分页列表
     */
    @GetMapping
    public OrderPageResult<DriverPayOrderPageVO> queryDriverPayOrderPage(@RequestAttribute("currentDriverId") Long driverId, @ModelAttribute DriverPayOrderPageQueryReq request) {
        return coldChainPayOrderService.queryDriverPayOrderPage(driverId, request);
    }
}
