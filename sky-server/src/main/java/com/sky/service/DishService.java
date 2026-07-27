package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    public void saveDishAndFlavor(DishDTO dishDTO);

    Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO);

    void delete(List<Long> ids);

    Result<DishVO> getById(Long id);

    void update(DishDTO dishDTO);

    Result<List<Dish>> list(Long categoryId);
}
