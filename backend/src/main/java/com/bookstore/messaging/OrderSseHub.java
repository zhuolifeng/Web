package com.bookstore.messaging;

import com.bookstore.dto.OrderStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * SSE 推送中心：维护「排队 ID → 订阅者(SseEmitter)」以及每个排队单的最新状态。
 * <p>
 * Kafka 消费者处理完订单后调用 {@link #publish}，即可把结果实时推送给所有订阅该订单的客户端。
 * 若客户端在处理完成之后才订阅，也能立即收到最终结果（避免竞态丢消息）。
 */
@Component
public class OrderSseHub {

    private static final Logger log = LoggerFactory.getLogger(OrderSseHub.class);
    public static final String EVENT_NAME = "order-status";

    private final long sseTimeoutMs;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "order-sse-cleaner");
        t.setDaemon(true);
        return t;
    });

    public OrderSseHub(@Value("${bookstore.order.sse-timeout-ms:300000}") long sseTimeoutMs) {
        this.sseTimeoutMs = sseTimeoutMs;
    }

    private static final class Entry {
        final Long ownerId;
        final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        volatile OrderStatusMessage latest;

        Entry(Long ownerId, OrderStatusMessage latest) {
            this.ownerId = ownerId;
            this.latest = latest;
        }
    }

    /** 下单时登记排队单（状态 QUEUED） */
    public void register(String queueId, Long ownerId) {
        entries.put(queueId, new Entry(ownerId,
                OrderStatusMessage.of(queueId, OrderStatusMessage.QUEUED, "订单已进入队列，等待处理")));
    }

    /** 投递 Kafka 失败时撤销登记 */
    public void unregister(String queueId) {
        entries.remove(queueId);
    }

    public boolean isOwnedBy(String queueId, Long userId) {
        Entry e = entries.get(queueId);
        return e != null && e.ownerId.equals(userId);
    }

    /** 建立 SSE 订阅：先立即推送当前状态，若已是终态则直接结束连接 */
    public SseEmitter subscribe(String queueId) {
        Entry entry = entries.get(queueId);
        SseEmitter emitter = new SseEmitter(sseTimeoutMs);
        if (entry == null) {
            emitter.complete();
            return emitter;
        }
        emitter.onCompletion(() -> entry.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            entry.emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> entry.emitters.remove(emitter));

        synchronized (entry) {
            OrderStatusMessage current = entry.latest;
            if (!send(emitter, current)) {
                return emitter;
            }
            if (current.isFinal()) {
                emitter.complete();
            } else {
                entry.emitters.add(emitter);
            }
        }
        return emitter;
    }

    /** 由 Kafka 消费者调用：更新状态并推送给所有订阅者；终态时关闭连接 */
    public void publish(OrderStatusMessage msg) {
        Entry entry = entries.get(msg.getQueueId());
        if (entry == null) {
            log.warn("收到未登记排队单的状态更新 queueId={}，忽略推送", msg.getQueueId());
            return;
        }
        synchronized (entry) {
            entry.latest = msg;
            for (SseEmitter emitter : entry.emitters) {
                if (send(emitter, msg) && msg.isFinal()) {
                    emitter.complete();
                }
            }
            if (msg.isFinal()) {
                entry.emitters.clear();
                // 保留最终结果 10 分钟，供迟到的订阅者查询
                cleaner.schedule(() -> entries.remove(msg.getQueueId()), 10, TimeUnit.MINUTES);
            }
        }
        log.info("SSE 推送 queueId={} status={}", msg.getQueueId(), msg.getStatus());
    }

    private boolean send(SseEmitter emitter, OrderStatusMessage msg) {
        try {
            emitter.send(SseEmitter.event()
                    .id(msg.getQueueId() + "-" + msg.getStatus())
                    .name(EVENT_NAME)
                    .data(msg, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException ex) {
            emitter.completeWithError(ex);
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
    }
}
