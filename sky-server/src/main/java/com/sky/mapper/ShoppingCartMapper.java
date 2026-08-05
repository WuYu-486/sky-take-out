package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    List<ShoppingCart> select(ShoppingCart shoppingCart);

    void update(ShoppingCart existShoppingCart);

    void insert(ShoppingCart shoppingCart);

    void insertBatch(@Param("list") List<ShoppingCart> shoppingCartList);

    @Select("SELECT id, name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time FROM shopping_cart WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void deleteById(Long userId);
}
