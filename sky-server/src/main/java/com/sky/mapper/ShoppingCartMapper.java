package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 条件查询购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> select(ShoppingCart shoppingCart);

    /**
     * 修改购物车商品
     * @param existShoppingCart
     */
    void update(ShoppingCart existShoppingCart);

    /**
     * 新增购物车商品
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 批量新增购物车商品（再来一单）
     * @param shoppingCartList
     */
    void insertBatch(@Param("list") List<ShoppingCart> shoppingCartList);

    /**
     * 根据用户id查询购物车列表
     * @param userId
     * @return
     */
    @Select("SELECT id, name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time FROM shopping_cart WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ShoppingCart> list(Long userId);

    /**
     * 根据用户id清空购物车
     * @param userId
     */
    @Delete("DELETE FROM shopping_cart WHERE user_id = #{userId}")
    void deleteById(Long userId);

    /**
     * 根据购物车记录id删除单条记录
     * @param id 购物车记录id
     */
    @Delete("DELETE FROM shopping_cart WHERE id = #{id}")
    void deleteByCartId(Long id);
}
