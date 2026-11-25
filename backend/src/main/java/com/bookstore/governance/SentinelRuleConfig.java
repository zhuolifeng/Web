package com.bookstore.governance;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.bookstore.service.impl.BookServiceImpl;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * hw7：Sentinel 限流 / 熔断规则。
 *
 * <ul>
 *   <li><b>限流</b>：Web 资源 {@code GET /api/v1/book/{id}}（Sentinel MVC 拦截器以 URL 模板作为资源名），
 *       QPS 阈值默认 5，超出直接拒绝 → {@link SentinelBlockHandler} 返回 HTTP 429。</li>
 *   <li><b>熔断</b>：方法资源 {@value BookServiceImpl#BOOK_DETAIL_RESOURCE}（{@code @SentinelResource}），
 *       统计窗口内异常比例超过阈值即熔断一段时间，期间直接走降级逻辑返回 503，不再访问缓存 / 数据库。</li>
 * </ul>
 * 阈值均可在 application.properties 中修改。
 */
@Configuration
public class SentinelRuleConfig {

    /** Sentinel Web 拦截器生成的资源名（Controller 上的 URL 模板） */
    public static final String BOOK_DETAIL_URL_RESOURCE = "/api/v1/book/{id}";

    private static final Logger log = LoggerFactory.getLogger(SentinelRuleConfig.class);

    @Value("${bookstore.sentinel.book-detail.qps:5}")
    private double bookDetailQps;

    @Value("${bookstore.sentinel.book-detail.degrade.exception-ratio:0.5}")
    private double degradeExceptionRatio;

    @Value("${bookstore.sentinel.book-detail.degrade.min-request-amount:5}")
    private int degradeMinRequestAmount;

    @Value("${bookstore.sentinel.book-detail.degrade.stat-interval-ms:10000}")
    private int degradeStatIntervalMs;

    @Value("${bookstore.sentinel.book-detail.degrade.time-window-seconds:10}")
    private int degradeTimeWindowSeconds;

    @PostConstruct
    public void initRules() {
        FlowRule flow = new FlowRule(BOOK_DETAIL_URL_RESOURCE);
        flow.setGrade(RuleConstant.FLOW_GRADE_QPS);          // 按 QPS 限流
        flow.setCount(bookDetailQps);                         // 每秒最多 N 个请求
        flow.setLimitApp(RuleConstant.LIMIT_APP_DEFAULT);     // 不区分调用来源
        flow.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT); // 超出直接拒绝（快速失败）
        FlowRuleManager.loadRules(List.of(flow));

        DegradeRule degrade = new DegradeRule(BookServiceImpl.BOOK_DETAIL_RESOURCE);
        degrade.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO); // 按异常比例熔断
        degrade.setCount(degradeExceptionRatio);
        degrade.setMinRequestAmount(degradeMinRequestAmount);
        degrade.setStatIntervalMs(degradeStatIntervalMs);
        degrade.setTimeWindow(degradeTimeWindowSeconds);              // 熔断时长（秒）
        DegradeRuleManager.loadRules(List.of(degrade));

        log.info("[Sentinel] 已加载限流规则: resource={} QPS<={}", BOOK_DETAIL_URL_RESOURCE, bookDetailQps);
        log.info("[Sentinel] 已加载熔断规则: resource={} 异常比例>{} (最少{}个请求/{}ms) → 熔断{}s",
                BookServiceImpl.BOOK_DETAIL_RESOURCE, degradeExceptionRatio,
                degradeMinRequestAmount, degradeStatIntervalMs, degradeTimeWindowSeconds);
    }
}
