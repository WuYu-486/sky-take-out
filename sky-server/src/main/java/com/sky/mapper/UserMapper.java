package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    void insert(User user);

    User getById(Long userId);

    /**
     * 根据时间区间统计用户数量
     *
     * @param beginTime 统计开始时间（可空）
     * @param endTime   统计结束时间（可空）
     * @return 用户数量
     */
    Integer countByMap(@Param("beginTime") LocalDateTime beginTime, @Param("endTime") LocalDateTime endTime);
}
