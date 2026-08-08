package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 数据统计服务
 */
public interface ReportService {

    /**
     * 统计指定时间区间内的营业额
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 营业额统计结果
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的用户数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 用户统计结果
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的订单数据
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 订单统计结果
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内销量排名前10的商品
     *
     * @param begin 开始日期
     * @param end   结束日期
     * @return 销量Top10统计结果
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 统计报表
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
