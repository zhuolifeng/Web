package com.bookstore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体。包含收货信息、支付方式、订单状态和总金额。
 * <p>
 * hw8 反范式设计：冗余存储 {@code userNickname}、{@code itemCount}、{@code bookSnapshot}，
 * 订单列表 / 后台订单管理只读 orders 单表即可展示“谁买了哪些书、共几件”，
 * 无需 JOIN users 与 order_item（用空间换时间）。
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_no", unique = true, length = 64)
    private String orderNo;

    @Column(length = 64)
    private String receiver;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String note;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 32)
    private String status;

    @Column(name = "payment", length = 32)
    private String payment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 【冗余】下单时用户昵称（快照，用户改昵称不影响历史订单） */
    @Column(name = "user_nickname", length = 64)
    private String userNickname;

    /** 【冗余】商品总件数 = SUM(order_item.quantity) */
    @Column(name = "item_count")
    private Integer itemCount;

    /** 【冗余】图书快照 JSON：[{bookId,title,cover,price,qty}] */
    @Column(name = "book_snapshot", columnDefinition = "json")
    private String bookSnapshot;

    public Order() {}

    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public String getBookSnapshot() { return bookSnapshot; }
    public void setBookSnapshot(String bookSnapshot) { this.bookSnapshot = bookSnapshot; }

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

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) { this.payment = payment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
