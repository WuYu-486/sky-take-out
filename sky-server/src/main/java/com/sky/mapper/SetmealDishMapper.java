package com.sky.mapper;

import com.sky.entity.SetmealDish;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    @Select("select count(*) from setmeal_dish where dish_id = #{dishId}")
    Integer countByDishId(Long dishId);

    Integer countByDishIds(@Param("ids") List<Long> ids);

    void save(List<SetmealDish> setmealDishes);

    //把这里的*改成具体值
    @Select("select id, setmeal_id, dish_id, name, price, copies from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    void deleteBySetmealIds(List<Long> ids);
}
