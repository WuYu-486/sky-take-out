package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

import java.time.LocalDate;

/**
 * 工作台服务
 */
public interface WorkspaceService {

    /**
     * 统计指定时间区间内的营业数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 营业数据
     */
    BusinessDataVO getBusinessData(LocalDate begin, LocalDate end);

    /**
     * 查询订单管理数据
     *
     * @return 订单概览数据
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询菜品总览
     *
     * @return 菜品总览数据
     */
    DishOverViewVO getDishOverView();

    /**
     * 查询套餐总览
     *
     * @return 套餐总览数据
     */
    SetmealOverViewVO getSetmealOverView();
}
