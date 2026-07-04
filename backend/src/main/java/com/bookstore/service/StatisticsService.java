package com.bookstore.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计业务接口（管理员专用）。
 * <p>
 * 提供书籍销量排行和用户消费排行两种聚合查询，
 * 均支持按日期范围过滤。
 */
public interface StatisticsService {

    /** 指定时间范围内各种书的销量情况，按销售量降序 */
    List<Map<String, Object>> bookSalesRanking(LocalDate startDate, LocalDate endDate);

    /** 指定时间范围内每个用户的累计消费，按总金额降序 */
    List<Map<String, Object>> userSpendingRanking(LocalDate startDate, LocalDate endDate);
}
