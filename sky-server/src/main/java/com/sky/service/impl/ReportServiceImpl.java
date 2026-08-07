package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务实现
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额
     * <p>
     * 营业额：状态为已完成(5)的订单金额合计，按天统计
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 营业额统计结果
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 入参校验：日期不能为空，且开始日期不能晚于结束日期
        if (begin == null || end == null) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
        if (begin.isAfter(end)) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        // 生成日期列表（begin 到 end 之间的每一天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }

        // 按天统计营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }

        // 封装返回结果，日期与营业额一一对应，逗号分隔
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * <p>
     * 新增用户数：当天注册的用户数量；总用户数：截至当天结束的累计用户数量
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 用户统计结果
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 入参校验：日期不能为空，且开始日期不能晚于结束日期
        if (begin == null || end == null) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
        if (begin.isAfter(end)) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        // 生成日期列表（begin 到 end 之间的每一天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }

        // 按天统计新增用户数和总用户数
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 当天新增用户数
            Integer newUser = userMapper.countByMap(beginTime, endTime);
            // 截至当天结束的总用户数
            Integer totalUser = userMapper.countByMap(null, endTime);

            newUserList.add(newUser == null ? 0 : newUser);
            totalUserList.add(totalUser == null ? 0 : totalUser);
        }

        // 封装返回结果，日期与用户数一一对应，逗号分隔
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * <p>
     * 有效订单：状态为已完成(5)的订单；订单完成率 = 有效订单数 / 订单总数
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 订单统计结果
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 入参校验：日期不能为空，且开始日期不能晚于结束日期
        if (begin == null || end == null) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
        if (begin.isAfter(end)) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        // 生成日期列表（begin 到 end 之间的每一天）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }

        // 按天统计订单总数和有效订单数
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 当天订单总数（不限状态）
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            // 当天有效订单数（已完成）
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount == null ? 0 : orderCount);
            validOrderCountList.add(validOrderCount == null ? 0 : validOrderCount);
        }

        // 汇总时间区间内的订单总数和有效订单数
        int totalOrderCount = 0;
        int validOrderCount = 0;
        for (int i = 0; i < orderCountList.size(); i++) {
            totalOrderCount += orderCountList.get(i);
            validOrderCount += validOrderCountList.get(i);
        }

        // 计算订单完成率，订单总数为 0 时完成率记为 0，避免除零
        double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (double) validOrderCount / totalOrderCount;

        // 封装返回结果，日期与订单数一一对应，逗号分隔
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据时间区间和订单状态统计订单数量
     *
     * @param begin  统计开始时间
     * @param end    统计结束时间
     * @param status 订单状态（null 表示不限状态）
     * @return 订单数量
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 统计指定时间区间内销量排名前10的商品
     * <p>
     * 仅统计状态为已完成(5)的订单中的商品销量，按销量降序取前10
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 销量Top10统计结果
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        // 入参校验：日期不能为空，且开始日期不能晚于结束日期
        if (begin == null || end == null) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }
        if (begin.isAfter(end)) {
            throw new BaseException(MessageConstant.PARAM_ERROR);
        }

        // 统计区间：开始日期的零点到结束日期的最后一刻
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 查询销量排名前10的商品
        List<GoodsSalesDTO> goodsSalesList = orderMapper.getSalesTop10(beginTime, endTime);

        // 分别提取商品名称和销量，逗号拼接
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        if (goodsSalesList != null && !goodsSalesList.isEmpty()) {
            for (GoodsSalesDTO goodsSales : goodsSalesList) {
                nameList.add(goodsSales.getName());
                numberList.add(goodsSales.getNumber());
            }
        }

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }
}
