package com.bookstore.messaging;

import com.bookstore.dto.OrderEvent;
import com.bookstore.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * KafkaProducer：把订单事件 JSON 投递到 order-events。
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              @Value("${bookstore.kafka.order-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    /**
     * 发送订单事件。等待 broker 确认（毫秒级）后返回，保证返回 202 时消息已经可靠入队；
     * 真正耗时的库存扣减由消费者异步完成，不会阻塞 HTTP 请求。
     */
    public void send(OrderEvent event) {
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "订单事件序列化失败");
        }
        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(topic, event.getQueueId(), json)
                    .get(10, TimeUnit.SECONDS);
            log.info("订单事件已投递 queueId={} topic={} partition={} offset={}",
                    event.getQueueId(), topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "订单队列暂不可用，请稍后重试");
        } catch (Exception e) {
            log.error("订单事件投递失败 queueId={}", event.getQueueId(), e);
            throw new BusinessException(503, "订单队列暂不可用，请稍后重试");
        }
    }
}
