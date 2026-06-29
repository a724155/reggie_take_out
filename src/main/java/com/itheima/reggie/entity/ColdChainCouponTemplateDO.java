package com.itheima.reggie.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 冷运优惠券模板。
 *
 * 这是平台配置的“券规则”。
 * 例如：满10减5、总共发1000张、领取后7天有效。
 */
@Data
public class ColdChainCouponTemplateDO {

    /**
     * 优惠券模板 ID。
     */
    private Long id;

    /**
     * 优惠券名称。
     */
    private String couponName;

    /**
     * 优惠券类型，1：满减券
     */
    private Integer couponType;

    /**
     * 优惠金额。
     */
    private BigDecimal discountAmount;

    /**
     * 使用门槛金额。
     */
    private BigDecimal thresholdAmount;

    /**
     * 总发券数量。
     */
    private Integer totalCount;

    /**
     * 剩余可领取数量。
     */
    private Integer remainCount;

    /**
     * 领取开始时间。
     */
    private LocalDateTime receiveStartTime;

    /**
     * 领取结束时间。
     */
    private LocalDateTime receiveEndTime;

    /**
     * 领取后有效天数。
     */
    private Integer validDays;

    /**
     * 模板状态：1启用，0停用。
     */
    private Integer templateStatus;
}
