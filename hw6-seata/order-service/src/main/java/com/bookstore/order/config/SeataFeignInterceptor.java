package com.bookstore.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 调用方：把当前线程上的全局事务 XID 写入 HTTP 头 {@code TX_XID}，
 * 下游库存中心据此把自己的本地事务注册为同一个全局事务的分支。
 */
@Component
public class SeataFeignInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SeataFeignInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {
        String xid = RootContext.getXID();
        if (StringUtils.hasText(xid)) {
            template.header(RootContext.KEY_XID, xid);
            log.info("[Seata] Feign 调用 {} 透传 XID={}", template.url(), xid);
        }
    }
}
