package com.itheima.reggie.common;


/**
 * 冷运优惠券业务错误码。
 *
 * 练习项目先使用整数常量。
 * 企业项目中通常会使用枚举统一维护。
 */
public final class ColdChainCouponErrorCodeConstants {

    /**
     * 优惠券模板不存在。
     */
    public static final Integer COUPON_TEMPLATE_NOT_FOUND = 40001;

    /**
     * 当前优惠券不可领取。
     */
    public static final Integer COUPON_NOT_CLAIMABLE = 40002;

    /**
     * 司机已经领取过该优惠券。
     */
    public static final Integer COUPON_ALREADY_CLAIMED = 40003;

    /**
     * 优惠券库存不足。
     */
    public static final Integer COUPON_STOCK_NOT_ENOUGH = 40004;

    /**
     * 优惠券不可使用。
     */
    public static final Integer COUPON_NOT_USABLE = 40005;

    /**
     * 优惠券已经被锁定或使用。
     */
    public static final Integer COUPON_ALREADY_LOCKED_OR_USED = 40006;

    /**
     * 优惠券金额配置异常
     */
    public static final Integer COUPON_AMOUNT_CONFIGURATION_ERROR = 40007;

    /**
     * 优惠券有效期配置异常
     */
    public static final Integer COUPON_VALIDITY_PERIOD_CONFIGURATION_ERROR = 40008;

    /**
     * 优惠券领取时间配置异常
     */
    public static final Integer COUPON_REDEMPTION_TIME_CONFIGURATION_ERROR = 40009;

    /**
     * 当前不在优惠券领取时间内
     */
    public static final Integer COUPON_NO_PERIOD = 4001;


    /**
     * 工具类禁止实例化。
     */
    private ColdChainCouponErrorCodeConstants() {
    }
}
