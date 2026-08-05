package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.BaseException;
import com.sky.constant.StatusConstant;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Transactional
    @Override
    public void saveDishAndFlavor(DishDTO dishDTO) {
        // 入参校验：菜品名称和分类不能为空
        if (dishDTO.getName() == null || dishDTO.getName().trim().isEmpty()) {
            throw new BaseException("菜品名称不能为空");
        }
        if (dishDTO.getCategoryId() == null) {
            throw new BaseException("菜品分类不能为空");
        }

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        Long id = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //判断非空
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> flavor.setDishId(id));
            dishFlavorMapper.insertFlavors(flavors);
        }

    }

    @Override
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return Result.success(new PageResult(page.getTotal(), page.getResult()));
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        // 入参校验：删除的菜品id不能为空
        if (ids == null || ids.isEmpty()) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        for (Long id : ids) {
            //1.菜品是否存在、是否起售中
            Dish dish = dishMapper.getById(id);
            if (dish == null) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_NOT_FOUND);
            }
            if (StatusConstant.ENABLE.equals(dish.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 2.菜品是否在套餐内
        Integer count = setmealDishMapper.countByDishIds(ids);
        if (count != null && count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //3.删除菜品关联口味
        dishFlavorMapper.deleteByDishIds(ids);

        //4.删除菜品
        dishMapper.deleteByIds(ids);

    }

    @Override
    public Result<DishVO> getById(Long id) {
        //通过id查询菜品数据
        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            throw new BaseException(MessageConstant.DISH_NOT_FOUND);
        }

        //通过菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return Result.success(dishVO);
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        if (dishDTO.getId() == null) {
            throw new BaseException("菜品id不能为空");
        }
        //根据id更新菜品数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //根据dishid先删除口味,在添加口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> flavor.setDishId(dishDTO.getId()));
            dishFlavorMapper.insertFlavors(flavors);
        }
    }

    @Override
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> dishList = dishMapper.list(categoryId);
        return Result.success(dishList);
    }

    @Override
    public void status(Integer status, Long id) {
        if (id == null || (!StatusConstant.ENABLE.equals(status) && !StatusConstant.DISABLE.equals(status))) {
            throw new BaseException("菜品状态参数错误");
        }
        Dish dish = new Dish();
        dish.setStatus(status);
        dish.setId(id);
        dishMapper.update(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.listWithFlavor(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}
