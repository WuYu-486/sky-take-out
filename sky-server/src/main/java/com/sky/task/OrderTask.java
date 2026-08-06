package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 * <p>
 * 1. 每分钟执行一次，自动取消下单超过 15 分钟仍未支付的订单
 * 2. 每天凌晨 1 点执行一次，自动完成预计送达时间已过但仍处于派送中的订单
 */
@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时未支付订单
     * <p>
     * 每分钟执行一次，将下单超过 15 分钟仍未支付的订单自动取消
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        LocalDateTime now = LocalDateTime.now();
        log.info("定时处理超时未支付订单, 当前时间: {}", now);

        // 下单时间早于当前时间 15 分钟的待付款订单视为超时
        LocalDateTime deadline = now.minusMinutes(15);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, deadline);

        if (ordersList != null && !ordersList.isEmpty()) {
            int successCount = 0;
            for (Orders orders : ordersList) {
                // 带状态条件更新，防止与用户支付/取消操作并发时覆盖订单状态
                successCount += orderMapper.cancelByCondition(
                        orders.getId(), Orders.PENDING_PAYMENT, Orders.CANCELLED, "订单超时,自动取消", now);
            }
            log.info("本次共自动取消超时订单 {} 笔", successCount);
        }
    }

    /**
     * 处理一直处于派送中的订单
     * <p>
     * 每天凌晨 1 点执行一次，将预计送达时间已过但仍处于派送中的订单自动标记为已完成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        LocalDateTime now = LocalDateTime.now();
        log.info("定时处理派送中订单, 当前时间: {}", now);

        // 预计送达时间已过但仍在派送中的订单视为已送达
        List<Orders> ordersList = orderMapper.getByStatusAndEstimatedDeliveryTimeLT(Orders.DELIVERY_IN_PROGRESS, now);

        if (ordersList != null && !ordersList.isEmpty()) {
            int successCount = 0;
            for (Orders orders : ordersList) {
                successCount += orderMapper.updateStatusByCondition(
                        orders.getId(), Orders.DELIVERY_IN_PROGRESS, Orders.COMPLETED);
            }
            log.info("本次共自动完成派送中订单 {} 笔", successCount);
        }
    }
}
