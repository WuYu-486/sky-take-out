package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 减少购物车中一个商品
     *
     * @param shoppingCartDTO 商品信息（菜品id或套餐id）
     */
    void sub(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> list();

    void delete();
}
