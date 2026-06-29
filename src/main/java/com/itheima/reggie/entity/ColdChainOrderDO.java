package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;


/**
 * 冷运订单数据库实体。
 *
 * 对应表：
 * cold_chain_order
 */
@Data
@TableName("cold_chain_order")
public class ColdChainOrderDO {

    /**
     * 冷运订单主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 冷运业务订单号。
     */
    private String orderNo;

    /**
     * 当前订单归属司机。
     */
    private Long driverId;

    /**
     * 货物名称。
     */
    private String cargoName;

    /**
     * 起始地。
     */
    private String startAddress;

    /**
     * 目的地。
     */
    private String endAddress;

    /**
     * 原始定金金额。
     */
    private BigDecimal depositAmount;

    /**
     * 订单状态。
     */
    private Integer orderStatus;

    /**
     * 货源是否被支付流程锁定。
     */
    private Integer cargoLockStatus;

    /**
     * 货源锁定过期时间。
     */
    private LocalDateTime cargoLockExpireTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
