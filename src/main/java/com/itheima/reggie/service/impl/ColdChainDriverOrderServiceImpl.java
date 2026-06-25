package com.itheima.reggie.service.impl;

import com.itheima.reggie.api.request.DriverCancelOrderReq;
import com.itheima.reggie.api.request.DriverOrderPageQueryReq;
import com.itheima.reggie.api.request.DriverPaySubmitReq;
import com.itheima.reggie.api.response.ColdChainOrderDetailVO;
import com.itheima.reggie.api.response.ColdChainOrderPageVO;
import com.itheima.reggie.api.response.DriverPayOrderVO;
import com.itheima.reggie.api.response.PaySubmitVO;
import com.itheima.reggie.common.PageResult;
import com.itheima.reggie.service.IColdChainDriverOrderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ColdChainDriverOrderServiceImpl implements IColdChainDriverOrderService {
    @Override
    public PageResult<ColdChainOrderPageVO> queryDriverOrderPage(Long driverId, DriverOrderPageQueryReq request) {
        return null;
    }

    @Override
    public ColdChainOrderDetailVO queryDriverOrderDetail(Long driverId, Long orderId) {
        return null;
    }

    @Override
    public List<DriverPayOrderVO> queryDriverPayOrderList(Long driverId, Long orderId, Boolean includeClosed) {
        return null;
    }

    @Override
    public PaySubmitVO submitDriverPay(Long driverId, Long orderId, DriverPaySubmitReq request, String idempotencyKey) {
        return null;
    }

    @Override
    public void cancelDriverOrder(Long driverId, Long orderId, DriverCancelOrderReq request) {

    }
}
