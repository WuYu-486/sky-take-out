package com.sky.mapper;

import com.sky.entity.SetmealDish;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealDishMapper {

    @Select("select count(id) from setmeal_dish where dish_id = #{dishId}")
    Integer countByDishId(Long dishId);
}
