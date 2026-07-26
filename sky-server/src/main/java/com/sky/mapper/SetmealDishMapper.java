package com.sky.mapper;

import com.sky.entity.SetmealDish;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    @Select("select count(*) from setmeal_dish where dish_id = #{dishId}")
    Integer countByDishId(Long dishId);

    Integer countByDishIds(@Param("ids") List<Long> ids);
}
