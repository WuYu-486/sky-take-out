package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 统计菜品被套餐关联的数量
     * @param ids 菜品id集合
     * @return
     */
    Integer countByDishIds(@Param("ids") List<Long> ids);

    /**
     * 批量保存套餐菜品
     * @param setmealDishes
     */
    void save(@Param("setmealDishes") List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id查询套餐内菜品
     * @param setmealId
     * @return
     */
    @Select("select id, setmeal_id, dish_id, name, price, copies from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    /**
     * 根据套餐id删除套餐内菜品
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * 根据套餐id集合批量删除套餐内菜品
     * @param ids
     */
    void deleteBySetmealIds(@Param("ids") List<Long> ids);
}
