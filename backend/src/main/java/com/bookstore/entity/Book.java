package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 128)
    private String author;

    @Column(length = 64)
    private String category;

    @Column(length = 32)
    private String badge;

    @Column(length = 16)
    private String stars;

    @Column(name = "rating_num", length = 16)
    private String ratingNum;

    @Column(name = "rating_count", length = 64)
    private String ratingCount;

    @Column(length = 32)
    private String price;

    @Column(name = "original_price", length = 32)
    private String originalPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(name = "author_bio", columnDefinition = "TEXT")
    private String authorBio;

    @Column(length = 128)
    private String publisher;

    @Column(name = "publish_date", length = 64)
    private String publishDate;

    @Column(length = 32)
    private String pages;

    @Column(length = 64)
    private String isbn;

    @Column(length = 32)
    private String binding;

    @Column(name = "cover_img", length = 255)
    private String coverImg;

    @Column(name = "cover_emoji", length = 16)
    private String coverEmoji;

    public Book() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getStars() { return stars; }
    public void setStars(String stars) { this.stars = stars; }

    public String getRatingNum() { return ratingNum; }
    public void setRatingNum(String ratingNum) { this.ratingNum = ratingNum; }

    public String getRatingCount() { return ratingCount; }
    public void setRatingCount(String ratingCount) { this.ratingCount = ratingCount; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(String originalPrice) { this.originalPrice = originalPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getAuthorBio() { return authorBio; }
    public void setAuthorBio(String authorBio) { this.authorBio = authorBio; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }

    public String getPages() { return pages; }
    public void setPages(String pages) { this.pages = pages; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getBinding() { return binding; }
    public void setBinding(String binding) { this.binding = binding; }

    public String getCoverImg() { return coverImg; }
    public void setCoverImg(String coverImg) { this.coverImg = coverImg; }

    public String getCoverEmoji() { return coverEmoji; }
    public void setCoverEmoji(String coverEmoji) { this.coverEmoji = coverEmoji; }
}
