package com.bookstore.order.config;

import feign.RequestTemplate;
import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeataFeignInterceptorTest {

    private final SeataFeignInterceptor interceptor = new SeataFeignInterceptor();

    @AfterEach
    void cleanup() {
        RootContext.unbind();
    }

    @Test
    void propagatesXidWhenInGlobalTransaction() {
        RootContext.bind("127.0.0.1:8091:123456");
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);
        assertEquals("127.0.0.1:8091:123456",
                template.headers().get(RootContext.KEY_XID).iterator().next());
    }

    @Test
    void noHeaderWithoutGlobalTransaction() {
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);
        assertFalse(template.headers().containsKey(RootContext.KEY_XID));
    }
}
