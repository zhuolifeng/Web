package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.BookDto;
import com.bookstore.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 书籍相关 REST 接口。
 * <p>
 * 全部通过 {@link BookService} 间接访问数据库，不直接暴露 JPA Entity，
 * 由 {@link BookDto} 屏蔽底层存储（当前 MySQL；下学期可能 MongoDB / Redis）。
 */
@RestController
@RequestMapping("/api/v1")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** GET /api/v1/books —— 获取所有书籍列表 */
    @GetMapping("/books")
    public ApiResponse<List<BookDto>> listAll() {
        return ApiResponse.ok(bookService.listAll());
    }

    /** GET /api/v1/book/{id} —— 获取单本书籍详情 */
    @GetMapping("/book/{id}")
    public ApiResponse<BookDto> getOne(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getById(id));
    }
}
