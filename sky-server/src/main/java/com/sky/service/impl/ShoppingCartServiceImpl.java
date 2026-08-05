package com.sky.service.impl;
import com.sky.context.BaseContext;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.ShoppingCart;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 购物车业务层
 */
@Service
@Slf4j
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
            throw new ShoppingCartBusinessException(MessageConstant.PARAM_ERROR);
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
                    throw new ShoppingCartBusinessException(MessageConstant.DISH_NOT_FOUND);
                }
                // 校验菜品是否在售
                if (!StatusConstant.ENABLE.equals(dish.getStatus())) {
                    throw new ShoppingCartBusinessException(MessageConstant.GOODS_NOT_ON_SALE);
                }
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                //添加的是套餐
                SetmealVO setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                if (setmeal == null) {
                    throw new ShoppingCartBusinessException(MessageConstant.SETMEAL_NOT_FOUND);
                }
                // 校验套餐是否在售
                if (!StatusConstant.ENABLE.equals(setmeal.getStatus())) {
                    throw new ShoppingCartBusinessException(MessageConstant.GOODS_NOT_ON_SALE);
                }
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(java.time.LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
            log.info("添加购物车成功：userId={}, dishId={}, setmealId={}", userID, dishId, shoppingCartDTO.getSetmealId());
        }

    }

    @Override
    public List<ShoppingCart> list() {
        Long currentId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartMapper.list(currentId);
        return list;
    }

    @Override
    @Transactional
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        // 入参校验
        if (shoppingCartDTO == null || (shoppingCartDTO.getDishId() == null && shoppingCartDTO.getSetmealId() == null)) {
            throw new ShoppingCartBusinessException(MessageConstant.PARAM_ERROR);
        }

        // 根据菜品id或套餐id查询当前用户的购物车记录
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.select(shoppingCart);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_ITEM_NOT_FOUND);
        }

        ShoppingCart existShoppingCart = shoppingCarts.get(0);
        if (existShoppingCart.getNumber() > 1) {
            // 数量大于1：数量减一
            existShoppingCart.setNumber(existShoppingCart.getNumber() - 1);
            shoppingCartMapper.update(existShoppingCart);
        } else {
            // 数量为1：删除该条购物车记录
            shoppingCartMapper.deleteByCartId(existShoppingCart.getId());
        }
        log.info("减少购物车商品成功：userId={}, dishId={}, setmealId={}", existShoppingCart.getUserId(),
                shoppingCartDTO.getDishId(), shoppingCartDTO.getSetmealId());
    }

    @Override
    public void delete() {
        shoppingCartMapper.deleteById(BaseContext.getCurrentId());
    }
}









