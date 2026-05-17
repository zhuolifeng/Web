package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.entity.Book;
import com.bookstore.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** GET /api/v1/books — list all books from MySQL */
    @GetMapping("/books")
    public ApiResponse<List<Book>> listAll() {
        return ApiResponse.ok(bookService.listAll());
    }

    /** GET /api/v1/book/{id} — fetch single book detail */
    @GetMapping("/book/{id}")
    public ApiResponse<Book> getOne(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getById(id));
    }
}
