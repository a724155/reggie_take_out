package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.extension.api.R;
import com.itheima.reggie.common.ColdChainRequestAttributeConstants;
import com.itheima.reggie.common.YmmResult;
import com.itheima.reggie.service.IColdChainDriverCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;

/**
 * 冷运司机优惠券 Controller。
 *
 * Controller 只做四件事：
 *
 * 1. 接收 HTTP 请求；
 * 2. 获取当前司机身份；
 * 3. 校验基础参数；
 * 4. 调用 Service 并返回结果。
 *
 * 不能把扣库存、插入优惠券、事务控制写在 Controller 中。
 */
@Validated
@RestController
@RequestMapping("/cold-chain/driver/coupons")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ColdChainDriverCouponController {

    private final IColdChainDriverCouponService coldChainDriverCouponService;

    /**
     * 司机领取优惠券。
     *
     * 请求示例：
     *
     * POST /cold-chain/driver/coupons/templates/1/claim
     *
     * @param driverId 当前登录司机 ID，由拦截器写入 request attribute
     * @param templateId 优惠券模板 ID
     * @return 司机优惠券 ID
     */
    @PostMapping("/templates/{templateId}/claim")
    public YmmResult<Long> claimCoupon(@RequestAttribute(ColdChainRequestAttributeConstants.CURRENT_DRIVER_ID) Long driverId,
            @PathVariable @Positive(message = "优惠券模板ID必须大于0") Long templateId) {

        Long driverCouponId = coldChainDriverCouponService.claimCoupon(driverId, templateId);
        return YmmResult.success(driverCouponId);
    }

}
