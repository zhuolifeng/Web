package com.bookstore.cloud;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Immutable teaching fixture: sleep simulates blocking database I/O. */
@Service
public class BookCatalog {
    public record Book(long id, String title, String author, String category, String price, int stock) {}
    private final long delayMs;
    private final Map<Long, Book> books = Map.of(
            1L, new Book(1L, "三体", "刘慈欣", "科幻", "59.80", 100),
            2L, new Book(2L, "微服务架构入门（模拟）", "模拟作者", "计算机", "69.00", 30));

    public BookCatalog(@Value("${bookstore.io-delay-ms:200}") long delayMs) {
        if (delayMs < 0 || delayMs > 5000) throw new IllegalArgumentException("I/O delay must be 0–5000 ms");
        this.delayMs = delayMs;
    }

    public Book find(long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id 必须大于等于 1");
        // No synchronized block or shared mutable session state around the sleep.
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "查询已中断");
        }
        Book book = books.get(id);
        if (book == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图书不存在");
        return book;
    }
}
