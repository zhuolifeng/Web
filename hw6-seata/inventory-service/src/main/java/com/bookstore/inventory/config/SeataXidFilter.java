package com.bookstore.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 被调用方：从 HTTP 头 {@code TX_XID} 取出全局事务 ID 并绑定到当前线程，
 * 使本服务的本地事务注册为该全局事务的分支事务（Branch）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataXidFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SeataXidFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String xid = request.getHeader(RootContext.KEY_XID);
        boolean bound = false;
        if (StringUtils.hasText(xid) && !xid.equals(RootContext.getXID())) {
            RootContext.bind(xid);
            bound = true;
            log.info("[Seata] 收到上游全局事务 XID={}，绑定到当前线程 {} {}", xid,
                    request.getMethod(), request.getRequestURI());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (bound && RootContext.getXID() != null) {
                RootContext.unbind();
            }
        }
    }
}
