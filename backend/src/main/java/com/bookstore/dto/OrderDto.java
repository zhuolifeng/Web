package com.bookstore.dto;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单数据传输对象。
 * 包含订单基本信息和订单明细列表，用于前端展示。
 */
public class OrderDto {

    private Long id;
    private Long userId;
    private String orderNo;
    private String receiver;
    private String phone;
    private String address;
    private String note;
    private String status;
    private String payment;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItem> items;

    public OrderDto() {}

    public static OrderDto from(Order order, List<OrderItem> items) {
        OrderDto dto = new OrderDto();
        dto.id = order.getId();
        dto.userId = order.getUserId();
        dto.orderNo = order.getOrderNo();
        dto.receiver = order.getReceiver();
        dto.phone = order.getPhone();
        dto.address = order.getAddress();
        dto.note = order.getNote();
        dto.status = order.getStatus();
        dto.payment = order.getPayment();
        dto.totalAmount = order.getTotalAmount();
        dto.createdAt = order.getCreatedAt();
        dto.items = items;
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) { this.payment = payment; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
