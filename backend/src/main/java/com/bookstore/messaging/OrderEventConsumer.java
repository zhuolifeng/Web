package com.bookstore.messaging;

import com.bookstore.dto.OrderDto;
import com.bookstore.dto.OrderEvent;
import com.bookstore.dto.OrderStatusMessage;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * KafkaConsumer：拉取 order-events 中的订单事件，模拟耗时的库存扣减，
 * 处理完成后通过 {@link OrderSseHub} 把成功/失败结果实时推送给客户端。
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final OrderSseHub sseHub;
    private final long processDelayMs;

    public OrderEventConsumer(ObjectMapper objectMapper,
                              OrderService orderService,
                              OrderSseHub sseHub,
                              @Value("${bookstore.order.process-delay-ms:3000}") long processDelayMs) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.sseHub = sseHub;
        this.processDelayMs = processDelayMs;
    }

    @KafkaListener(topics = "${bookstore.kafka.order-topic}",
                   groupId = "${bookstore.kafka.consumer-group}")
    public void onOrderEvent(ConsumerRecord<String, String> record) {
        log.info("消费订单事件 key={} partition={} offset={} thread={}",
                record.key(), record.partition(), record.offset(), Thread.currentThread().getName());

        OrderEvent event;
        try {
            event = objectMapper.readValue(record.value(), OrderEvent.class);
        } catch (Exception e) {
            log.error("无法解析的订单事件，已跳过 offset={}", record.offset(), e);
            return;
        }
        String queueId = event.getQueueId();
        sseHub.publish(OrderStatusMessage.of(queueId, OrderStatusMessage.PROCESSING, "正在扣减库存…"));

        OrderStatusMessage result;
        try {
            // 模拟高并发促销下扣减库存的耗时操作
            Thread.sleep(processDelayMs);
            OrderDto order = orderService.createFromItems(event.getUserId(), event.getShipping(), event.getItems());
            result = OrderStatusMessage.of(queueId, OrderStatusMessage.SUCCESS, "下单成功，库存已扣减");
            result.setOrderId(order.getId());
            result.setOrderNo(order.getOrderNo());
            result.setTotalAmount(order.getTotalAmount());
        } catch (BusinessException e) {
            result = OrderStatusMessage.of(queueId, OrderStatusMessage.FAILED, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = OrderStatusMessage.of(queueId, OrderStatusMessage.FAILED, "订单处理被中断");
        } catch (Exception e) {
            log.error("订单处理异常 queueId={}", queueId, e);
            result = OrderStatusMessage.of(queueId, OrderStatusMessage.FAILED, "订单处理失败：系统异常");
        }
        log.info("订单处理完成 queueId={} status={} message={}", queueId, result.getStatus(), result.getMessage());
        sseHub.publish(result);
    }
}
