package com.itheima.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.reggie.entity.ColdChainPayOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 冷运定金支付单 Mapper
 *
 * 继承 BaseMapper 后，可以直接使用 MyBatis-Plus 内置方法。
 *
 * 常用方法：
 * selectById
 * selectList
 * selectPage
 * insert
 * updateById
 * update
 * deleteById
 */
@Mapper
public interface IColdChainPayOrderMapperPlus extends BaseMapper<ColdChainPayOrderDO> {
}
