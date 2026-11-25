package com.bookstore.governance;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.bookstore.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * hw7：Sentinel Web 拦截器的统一拦截响应。
 * <p>
 * 限流（FlowException）→ HTTP 429 Too Many Requests；熔断（DegradeException）→ HTTP 503 + 降级提示。
 * 响应体沿用项目统一的 {@link ApiResponse} JSON 结构。
 */
@Component
public class SentinelBlockHandler implements BlockExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SentinelBlockHandler.class);

    private final ObjectMapper objectMapper;

    public SentinelBlockHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        int status;
        String message;
        if (e instanceof DegradeException) {
            status = HttpStatus.SERVICE_UNAVAILABLE.value();
            message = "服务熔断降级中，请稍后重试";
        } else if (e instanceof FlowException) {
            status = HttpStatus.TOO_MANY_REQUESTS.value();
            message = "请求过于频繁，已被限流（Too Many Requests），请稍后重试";
        } else {
            status = HttpStatus.TOO_MANY_REQUESTS.value();
            message = "请求被系统保护规则拦截，请稍后重试";
        }
        log.debug("[Sentinel] {} {} 被拦截: {}", request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName());
        response.setStatus(status);
        response.setHeader("Retry-After", "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(status, message));
    }
}
