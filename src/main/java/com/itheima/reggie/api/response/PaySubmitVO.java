package com.itheima.reggie.api.response;

import lombok.Data;

/**
 * 冷运司机提交支付后的收银台信息。
 */
@Data
public class PaySubmitVO {

    /**
     * 本地支付单号。
     */
    private String payOrderNo;

    /**
     * 支付渠道。
     */
    private String payChannel;

    /**
     * 收银台拉起地址或支付凭证。
     *
     * App 可能根据该字段跳转支付宝、微信或平台收银台。
     */
    private String cashierUrl;

    /**
     * 支付截止时间。
     *
     * 为了聚焦 Controller 练习，这里先使用字符串；
     * 真实项目可统一使用 LocalDateTime 并配置 JSON 时间格式。
     */
    private String payExpireTime;
}