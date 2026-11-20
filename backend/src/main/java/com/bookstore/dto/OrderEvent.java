package com.bookstore.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 投递到 Kafka Topic {@code order-events} 的订单事件（JSON）。
 * <p>
 * 下单请求到达时立即对购物车做一次快照，消费者据此异步扣减库存并生成订单。
 */
public class OrderEvent {

    /** 排队 ID，同时作为 Kafka 消息 key 与 SSE 订阅 ID */
    private String queueId;
    private Long userId;
    private String username;
    private OrderCreateRequest shipping;
    private List<Item> items = new ArrayList<>();
    private LocalDateTime submittedAt;

    public static class Item {
        private Long bookId;
        private Integer quantity;

        public Item() {}

        public Item(Long bookId, Integer quantity) {
            this.bookId = bookId;
            this.quantity = quantity;
        }

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public OrderCreateRequest getShipping() { return shipping; }
    public void setShipping(OrderCreateRequest shipping) { this.shipping = shipping; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
