package com.itheima.reggie.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * MyBatis-Plus 公共字段自动填充处理器
 *
 * 负责统一填充：
 * createTime
 * updateTime
 */
@Component
public class CommonMetaObjectHandler implements MetaObjectHandler {

    /**
     * 新增数据时自动填充字段
     *
     * @param metaObject MyBatis 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime currentTime = LocalDateTime.now();

        /**
         * 仅当字段当前没有值时才填充。
         *
         * 防止业务代码已经主动设置创建时间时被覆盖。
         */
        this.strictInsertFill(
                metaObject,
                "createTime",
                LocalDateTime.class,
                currentTime
        );

        this.strictInsertFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                currentTime
        );
    }

    /**
     * 更新数据时自动填充字段
     *
     * @param metaObject MyBatis 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
    }
}
