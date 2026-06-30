package com.itheima.reggie.service;


/**
 * 冷运司机优惠券服务。
 */
public interface IColdChainDriverCouponService {

    /**
     * 司机领取优惠券。
     *
     * @param driverId 当前登录司机 ID
     * @param templateId 优惠券模板 ID
     * @return 领取成功后生成的司机优惠券 ID
     */
    Long claimCoupon(Long driverId, Long templateId);
}
