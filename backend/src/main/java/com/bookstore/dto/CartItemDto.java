package com.bookstore.dto;

import java.math.BigDecimal;

/**
 * Cart item enriched with book info — what the frontend cart page renders.
 */
public class CartItemDto {

    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private String price;          // original "¥xx.xx" string from books table
    private String originalPrice;
    private String coverImg;
    private String coverEmoji;
    private Integer quantity;
    private BigDecimal unitPrice;  // numeric form for subtotal math
    private BigDecimal subtotal;

    public CartItemDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(String originalPrice) { this.originalPrice = originalPrice; }

    public String getCoverImg() { return coverImg; }
    public void setCoverImg(String coverImg) { this.coverImg = coverImg; }

    public String getCoverEmoji() { return coverEmoji; }
    public void setCoverEmoji(String coverEmoji) { this.coverEmoji = coverEmoji; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
