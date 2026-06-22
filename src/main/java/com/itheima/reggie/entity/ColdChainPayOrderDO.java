package com.itheima.reggie.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cold_chain_pay_order")
public class ColdChainPayOrderDO {

    /**
     * 支付单主键ID。
     *
     * ASSIGN_ID 表示由 MyBatis-Plus 自动生成 Long 类型主键。
     * 默认策略通常基于雪花算法生成分布式 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 支付单号。
     *
     * 注意：
     * 这个通常由支付中心或业务系统单独生成，
     * 不建议直接把数据库 ID 当成对外支付单号。
     */
    private String payOrderNo;

    /**
     * 冷运业务订单ID。
     */
    private Long businessOrderId;

    /**
     * 货源ID。
     */
    private Long cargoId;

    /**
     * 司机ID。
     */
    private Long driverId;

    /**
     * 定金金额。
     *
     * 金额必须使用 BigDecimal，
     * 不允许使用 double 或 float，避免精度问题。
     */
    private BigDecimal payAmount;

    /**
     * 支付状态。
     *
     * 10：待支付
     * 20：已支付
     * 30：已关闭
     */
    private Integer payStatus;

    /**
     * 支付截止时间。
     */
    private LocalDateTime payExpireTime;

    /**
     * 实际支付成功时间。
     */
    private LocalDateTime paidTime;

    /**
     * 关闭原因。
     *
     * 例如：
     * PAY_TIMEOUT
     * DRIVER_CANCEL
     * RISK_CLOSE
     */
    private String closeReason;

    /**
     * 创建时间。
     *
     * 插入数据时由 MetaObjectHandler 自动填充。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间。
     *
     * 插入和更新时由 MetaObjectHandler 自动填充。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
