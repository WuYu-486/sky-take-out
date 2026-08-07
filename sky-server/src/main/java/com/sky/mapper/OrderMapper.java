package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理端条件搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计各状态订单数量
     * @return
     */
    List<Map<String, Object>> countByStatus();

    /**
     * 根据状态和下单时间查询订单（下单时间早于指定时间）
     * 用于定时任务处理超时未支付订单
     *
     * @param status 订单状态
     * @param time   截止时间
     * @return 符合条件的订单列表
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime time);

    /**
     * 根据状态和预计送达时间查询订单（预计送达时间早于指定时间）
     * 用于定时任务处理一直处于派送中的订单
     *
     * @param status 订单状态
     * @param time   截止时间
     * @return 符合条件的订单列表
     */
    @Select("select * from orders where status = #{status} and estimated_delivery_time < #{time}")
    List<Orders> getByStatusAndEstimatedDeliveryTimeLT(Integer status, LocalDateTime time);

    /**
     * 按原状态条件更新订单状态，返回受影响行数
     * 只有订单仍处于 oldStatus 时才会更新，避免定时任务并发覆盖用户已变更的订单
     *
     * @param id       订单id
     * @param oldStatus 原订单状态
     * @param newStatus 目标订单状态
     * @return 受影响行数（0 表示订单状态已变化，无需处理）
     */
    @Update("update orders set status = #{newStatus} where id = #{id} and status = #{oldStatus}")
    int updateStatusByCondition(Long id, Integer oldStatus, Integer newStatus);

    /**
     * 按原状态条件取消订单并写入取消原因、取消时间，返回受影响行数
     * 只有订单仍处于 oldStatus 时才会取消，避免定时任务并发覆盖用户已支付的订单
     *
     * @param id           订单id
     * @param oldStatus    原订单状态
     * @param newStatus    目标订单状态
     * @param cancelReason 取消原因
     * @param cancelTime   取消时间
     * @return 受影响行数（0 表示订单状态已变化，无需处理）
     */
    @Update("update orders set status = #{newStatus}, cancel_reason = #{cancelReason}, cancel_time = #{cancelTime} " +
            "where id = #{id} and status = #{oldStatus}")
    int cancelByCondition(Long id, Integer oldStatus, Integer newStatus, String cancelReason, LocalDateTime cancelTime);

    /**
     * 根据动态条件统计营业额
     * 条件：下单时间范围（beginTime、endTime）、订单状态（status）
     *
     * @param map 查询条件
     * @return 营业额合计，无数据时返回 null
     */
    Double sumByMap(Map<String, Object> map);

    /**
     * 根据动态条件统计订单数量
     * 条件：下单时间范围（begin、end）、订单状态（status）
     *
     * @param map 查询条件
     * @return 订单数量
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 查询时间区间内销量排名前10的商品
     *
     * @param beginTime 统计开始时间
     * @param endTime   统计结束时间
     * @return 商品销量列表（按销量降序）
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
