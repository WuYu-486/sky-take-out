package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    List<ShoppingCart> select(ShoppingCart shoppingCart);

    void update(ShoppingCart existShoppingCart);

    void insert(ShoppingCart shoppingCart);
}
