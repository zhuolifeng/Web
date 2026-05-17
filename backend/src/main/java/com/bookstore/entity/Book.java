package com.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
