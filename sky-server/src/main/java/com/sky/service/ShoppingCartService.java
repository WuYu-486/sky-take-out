package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

public interface ShoppingCartService {
    void add(ShoppingCartDTO shoppingCartDTO);
}
