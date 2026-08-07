package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作台服务实现
 */
@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 统计指定时间区间内的营业数据
     * <p>
     * 营业额：已完成订单金额合计；有效订单：已完成订单；
     * 订单完成率 = 有效订单数 / 订单总数；平均客单价 = 营业额 / 有效订单数
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 营业数据
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDate begin, LocalDate end) {
        // 入参校验：日期不能为空，且开始日期不能晚于结束日期
        if (begin == null || end == null) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
        if (begin.isAfter(end)) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 订单总数（不限状态）
        Integer totalOrderCount = orderMapper.countByMap(buildOrderMap(beginTime, endTime, null));
        // 有效订单数（已完成）
        Integer validOrderCount = orderMapper.countByMap(buildOrderMap(beginTime, endTime, Orders.COMPLETED));

        // 营业额（已完成订单金额合计）
        Map<String, Object> sumMap = new HashMap<>();
        sumMap.put("beginTime", beginTime);
        sumMap.put("endTime", endTime);
        sumMap.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.sumByMap(sumMap);
        turnover = turnover == null ? 0.0 : turnover;

        // 订单完成率 = 有效订单数 / 订单总数，避免除零
        double orderCompletionRate = 0.0;
        if (totalOrderCount != null && totalOrderCount != 0 && validOrderCount != null) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        // 平均客单价 = 营业额 / 有效订单数，避免除零
        double unitPrice = 0.0;
        if (turnover != 0 && validOrderCount != null && validOrderCount != 0) {
            unitPrice = turnover / validOrderCount;
        }

        // 新增用户数
        Integer newUsers = userMapper.countByMap(beginTime, endTime);
        newUsers = newUsers == null ? 0 : newUsers;

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount == null ? 0 : validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 构建订单统计查询条件
     * countByMap 的动态 SQL 使用 begin/end/status 作为条件 key
     *
     * @param beginTime 统计开始时间
     * @param endTime   统计结束时间
     * @param status    订单状态（null 表示不限状态）
     * @return 查询条件
     */
    private Map<String, Object> buildOrderMap(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return map;
    }

    /**
     * 查询订单管理数据
     * <p>
     * 待派送：已接单(3)但尚未派送的订单
     *
     * @return 订单概览数据
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        // 待接单数量（状态2）
        Integer waitingOrders = getCountByStatus(Orders.TO_BE_CONFIRMED);
        // 待派送数量（状态3 已接单）
        Integer deliveredOrders = getCountByStatus(Orders.CONFIRMED);
        // 已完成数量（状态5）
        Integer completedOrders = getCountByStatus(Orders.COMPLETED);
        // 已取消数量（状态6）
        Integer cancelledOrders = getCountByStatus(Orders.CANCELLED);
        // 全部订单（不限状态）
        Integer allOrders = getCountByStatus(null);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 根据订单状态统计数量
     *
     * @param status 订单状态（null 表示全部订单）
     * @return 订单数量
     */
    private Integer getCountByStatus(Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);
        Integer count = orderMapper.countByMap(map);
        return count == null ? 0 : count;
    }

    /**
     * 查询菜品总览
     *
     * @return 菜品总览数据
     */
    @Override
    public DishOverViewVO getDishOverView() {
        // 已启售菜品数量（状态1）
        Integer sold = dishMapper.countByStatus(1);
        // 已停售菜品数量（状态0）
        Integer discontinued = dishMapper.countByStatus(0);

        return DishOverViewVO.builder()
                .sold(sold == null ? 0 : sold)
                .discontinued(discontinued == null ? 0 : discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return 套餐总览数据
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        // 已启售套餐数量（状态1）
        Integer sold = setmealMapper.countByStatus(1);
        // 已停售套餐数量（状态0）
        Integer discontinued = setmealMapper.countByStatus(0);

        return SetmealOverViewVO.builder()
                .sold(sold == null ? 0 : sold)
                .discontinued(discontinued == null ? 0 : discontinued)
                .build();
    }
}
