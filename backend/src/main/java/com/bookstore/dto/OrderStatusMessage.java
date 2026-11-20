package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通过 SSE 推送给客户端的订单处理状态。
 * <p>
 * 状态流转：QUEUED → PROCESSING → SUCCESS / FAILED。
 */
public class OrderStatusMessage {

    public static final String QUEUED = "QUEUED";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private String queueId;
    private String status;
    private String message;
    private Long orderId;
    private String orderNo;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;

    public OrderStatusMessage() {}

    public static OrderStatusMessage of(String queueId, String status, String message) {
        OrderStatusMessage m = new OrderStatusMessage();
        m.queueId = queueId;
        m.status = status;
        m.message = message;
        m.timestamp = LocalDateTime.now();
        return m;
    }

    @JsonIgnore
    public boolean isFinal() {
        return SUCCESS.equals(status) || FAILED.equals(status);
    }

    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
