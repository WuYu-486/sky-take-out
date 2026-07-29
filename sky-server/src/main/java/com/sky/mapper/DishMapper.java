package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

import static com.sky.enumeration.OperationType.INSERT;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    @AutoFill(value = INSERT)
    @Insert("insert into dish (name, category_id, price, image, description, status, " +
            "create_time, update_time, create_user, update_user) " +
            "values (#{name}, #{categoryId}, #{price}, #{image}, #{description}, #{status}, " +
            "#{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Dish dish);

    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    @Select("select id, name, category_id, price, image, description, status, " +
            "create_time, update_time, create_user, update_user from dish where id = #{id}")
    Dish getById(Long id);

    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    void deleteByIds(@Param("ids") List<Long> ids);

    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    @Select("select id, name, category_id, price, image, description, status, " +
            "create_time, update_time, create_user, update_user from dish where category_id = #{categoryId} and status = 1")
    List<Dish> list(Long categoryId);

    List<Dish> listWithFlavor(Dish dish);
}

