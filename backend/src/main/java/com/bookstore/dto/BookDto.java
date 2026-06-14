package com.bookstore.dto;

import com.bookstore.entity.Book;

/**
 * Book 数据传输对象。
 *
 * 作用：屏蔽底层 Entity / 持久化实现细节，对外只暴露稳定的 JSON 结构。
 * 当下学期把数据存放到多种异构数据库（MySQL / MongoDB / Redis ...）时，
 * 只需要替换 Service 的实现 + 增加新的 from(xxxEntity) 工厂方法，
 * Controller 与前端契约保持不变。
 */
public class BookDto {

    private Long id;
    private String title;
    private String author;
    private String category;
    private String badge;
    private String stars;
    private String ratingNum;
    private String ratingCount;
    private String price;
    private String originalPrice;
    private String description;
    private String intro;
    private String authorBio;
    private String publisher;
    private String publishDate;
    private String pages;
    private String isbn;
    private String binding;
    private String coverImg;
    private String coverEmoji;

    public BookDto() {}

    public static BookDto from(Book book) {
        if (book == null) return null;
        BookDto dto = new BookDto();
        dto.id = book.getId();
        dto.title = book.getTitle();
        dto.author = book.getAuthor();
        dto.category = book.getCategory();
        dto.badge = book.getBadge();
        dto.stars = book.getStars();
        dto.ratingNum = book.getRatingNum();
        dto.ratingCount = book.getRatingCount();
        dto.price = book.getPrice();
        dto.originalPrice = book.getOriginalPrice();
        dto.description = book.getDescription();
        dto.intro = book.getIntro();
        dto.authorBio = book.getAuthorBio();
        dto.publisher = book.getPublisher();
        dto.publishDate = book.getPublishDate();
        dto.pages = book.getPages();
        dto.isbn = book.getIsbn();
        dto.binding = book.getBinding();
        dto.coverImg = book.getCoverImg();
        dto.coverEmoji = book.getCoverEmoji();
        return dto;
    }

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
