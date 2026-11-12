package com.bookstore.cloud;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookCatalog catalog;
    public BookController(BookCatalog catalog) { this.catalog = catalog; }

    @GetMapping("/{id}")
    public ApiResponse<BookCatalog.Book> getBook(@PathVariable long id) {
        return ApiResponse.ok(catalog.find(id));
    }
}
