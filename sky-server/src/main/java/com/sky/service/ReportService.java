package com.sky.service;

import com.sky.vo.TurnoverReportVO;

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
}
