package com.itheima.reggie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.reggie.entity.ColdChainCouponTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 冷运优惠券模板 Mapper。
 */
@Mapper
public interface IColdChainCouponTemplateMapper extends BaseMapper<ColdChainCouponTemplateDO> {

    /**
     * 原子扣减优惠券模板库存。
     *
     * 核心防超发 SQL：
     *
     * remain_count = remain_count - 1
     * and remain_count > 0
     *
     * 即使 100 个司机同时领最后一张券，
     * 最终也只会有一个请求更新成功，返回值为 1。
     *
     * @param templateId 优惠券模板 ID
     * @return 1：扣减成功；0：库存不足、模板停用或不在领取时间内
     */
    @Update("UPDATE cold_chain_coupon_template " +
            "SET remain_count = remain_count - 1 " +
            "WHERE id = #{templateId} " +
            "AND template_status = 1 " +
            "AND remain_count > 0 " +
            "AND receive_start_time <= NOW() " +
            "AND receive_end_time > NOW()")
    int decreaseRemainCountForClaim(@Param("templateId") Long templateId);
}
