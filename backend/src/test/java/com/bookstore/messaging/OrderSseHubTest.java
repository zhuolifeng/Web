package com.bookstore.messaging;

import com.bookstore.dto.OrderStatusMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

/** OrderSseHub 单元测试：排队单登记、归属校验与订阅。 */
class OrderSseHubTest {

    @Test
    @DisplayName("登记后仅归属用户可订阅")
    void ownership() {
        OrderSseHub hub = new OrderSseHub(1000);
        hub.register("q1", 7L);
        assertTrue(hub.isOwnedBy("q1", 7L));
        assertFalse(hub.isOwnedBy("q1", 8L));
        assertFalse(hub.isOwnedBy("missing", 7L));
        hub.unregister("q1");
        assertFalse(hub.isOwnedBy("q1", 7L));
    }

    @Test
    @DisplayName("未知排队单的状态推送被忽略，不抛异常")
    void publishUnknownIgnored() {
        OrderSseHub hub = new OrderSseHub(1000);
        assertDoesNotThrow(() -> hub.publish(
                OrderStatusMessage.of("nope", OrderStatusMessage.SUCCESS, "ok")));
    }

    @Test
    @DisplayName("订阅返回 SseEmitter，终态后仍保留结果供迟到订阅者读取")
    void subscribeAfterFinal() {
        OrderSseHub hub = new OrderSseHub(1000);
        hub.register("q2", 1L);
        hub.publish(OrderStatusMessage.of("q2", OrderStatusMessage.FAILED, "库存不足"));
        SseEmitter emitter = hub.subscribe("q2");
        assertNotNull(emitter);
        assertTrue(hub.isOwnedBy("q2", 1L));
    }
}
