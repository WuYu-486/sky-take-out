package com.sky.service.impl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.ShoppingCart;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    @Transactional
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //入参校验
        if (shoppingCartDTO.getDishId() == null && shoppingCartDTO.getSetmealId() == null) {
            throw new ShoppingCartBusinessException("请选择要添加的商品");
        }

        //判断当前添加购物车商品是否已有
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userID = BaseContext.getCurrentId();
        shoppingCart.setUserId(userID);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.select(shoppingCart);
        if(shoppingCarts != null && !shoppingCarts.isEmpty()){
            //已有:商品+1
            ShoppingCart existShoppingCart = shoppingCarts.get(0);
            existShoppingCart.setNumber(existShoppingCart.getNumber() + 1);
            shoppingCartMapper.update(existShoppingCart);
        }else{
            //无:新增购物车商品

            //判断添加的是菜品还是套餐
            Long dishId = shoppingCartDTO.getDishId();
            if(dishId != null){
                //添加的是菜品
                Dish dish = dishMapper.getById(dishId);
                if (dish == null) {
                    throw new ShoppingCartBusinessException("该菜品不存在");
                }
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                //添加的是套餐
                SetmealVO setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                if (setmeal == null) {
                    throw new ShoppingCartBusinessException("该套餐不存在");
                }
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(java.time.LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }

    }
}









