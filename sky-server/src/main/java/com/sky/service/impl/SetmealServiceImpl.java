package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BaseException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.entity.Dish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    SetmealMapper setmealMapper;

    @Autowired
    SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 入参校验：套餐名称和分类不能为空
        if (setmealDTO.getName() == null || setmealDTO.getName().trim().isEmpty()) {
            throw new BaseException("套餐名称不能为空");
        }
        if (setmealDTO.getCategoryId() == null) {
            throw new BaseException("套餐分类不能为空");
        }

        //套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.save(setmeal);

        //套餐内菜品信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes == null || setmealDishes.isEmpty()) {
            return;
        }
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmeal.getId()));
        setmealDishMapper.save(setmealDishes);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public SetmealVO getById(Long id) {
        //根据id查询套餐信息
        SetmealVO setmealVO = setmealMapper.getById(id);
        if (setmealVO == null) {
            throw new BaseException(MessageConstant.SETMEAL_NOT_FOUND);
        }

        //根据id查询套餐菜品信息
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        if (setmealDTO.getId() == null) {
            throw new BaseException("套餐id不能为空");
        }

        //修改套餐信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //删除套餐菜品信息
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            setmealDishes.forEach(dish -> dish.setSetmealId(setmealDTO.getId()));
            setmealDishMapper.save(setmealDishes);
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        // 入参校验
        if (ids == null || ids.isEmpty()) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        // 起售中的套餐不能删除
        for (Long id : ids) {
            SetmealVO setmealVO = setmealMapper.getById(id);
            if (setmealVO == null) {
                throw new BaseException(MessageConstant.SETMEAL_NOT_FOUND);
            }
            if (StatusConstant.ENABLE.equals(setmealVO.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //根据id删除套餐信息
        setmealMapper.delete(ids);

        //根据套餐id删除套餐菜品信息
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    @Override
    public void status(Integer status, Long id) {
        if (id == null || (!StatusConstant.ENABLE.equals(status) && !StatusConstant.DISABLE.equals(status))) {
            throw new BaseException("套餐状态参数错误");
        }
        // 校验套餐存在
        SetmealVO setmealVO = setmealMapper.getById(id);
        if (setmealVO == null) {
            throw new BaseException(MessageConstant.SETMEAL_NOT_FOUND);
        }

        // 启售时校验套餐内菜品全部在售
        if (StatusConstant.ENABLE.equals(status)) {
            List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
            if (setmealDishes != null && !setmealDishes.isEmpty()) {
                for (SetmealDish setmealDish : setmealDishes) {
                    Dish dish = dishMapper.getById(setmealDish.getDishId());
                    if (dish == null || !StatusConstant.ENABLE.equals(dish.getStatus())) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }

        Setmeal setmeal = new Setmeal();
        setmeal.setStatus(status);
        setmeal.setId(id);
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
