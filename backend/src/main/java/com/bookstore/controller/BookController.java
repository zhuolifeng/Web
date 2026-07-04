package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.BookDto;
import com.bookstore.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 书籍相关 REST 接口。
 * <p>
 * 公开接口（浏览/搜索）+ 管理员接口（增删改），
 * 全部通过 {@link BookService} 间接访问数据库，不直接暴露 JPA Entity。
 */
@RestController
@RequestMapping("/api/v1")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** GET /api/v1/books — 获取所有书籍列表，支持按关键词搜索 */
    @GetMapping("/books")
    public ApiResponse<List<BookDto>> listAll(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return ApiResponse.ok(bookService.search(keyword));
        }
        return ApiResponse.ok(bookService.listAll());
    }

    /** GET /api/v1/book/{id} — 获取单本书籍详情 */
    @GetMapping("/book/{id}")
    public ApiResponse<BookDto> getOne(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getById(id));
    }

    /** POST /api/v1/admin/books — 管理员新增书籍 */
    @PostMapping("/admin/books")
    public ApiResponse<BookDto> create(@RequestBody BookDto dto) {
        return ApiResponse.ok("添加成功", bookService.create(dto));
    }

    /** PUT /api/v1/admin/books/{id} — 管理员更新书籍 */
    @PutMapping("/admin/books/{id}")
    public ApiResponse<BookDto> update(@PathVariable Long id, @RequestBody BookDto dto) {
        return ApiResponse.ok("更新成功", bookService.update(id, dto));
    }

    /** DELETE /api/v1/admin/books/{id} — 管理员删除书籍 */
    @DeleteMapping("/admin/books/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
