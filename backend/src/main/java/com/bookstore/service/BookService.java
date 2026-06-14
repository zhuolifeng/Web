package com.bookstore.service;

import com.bookstore.dto.BookDto;

import java.util.List;

/**
 * 书籍业务接口。返回值统一使用 {@link BookDto}，
 * 实现类持有底层 {@link com.bookstore.repository.BookRepository}（JPA），
 * Controller 永远不会感知到 Entity 的存在，便于未来切换为多源/异构存储。
 */
public interface BookService {

    /** 列出所有书籍 —— 对应 GET /api/v1/books */
    List<BookDto> listAll();

    /** 按主键查单本 —— 对应 GET /api/v1/book/{id} */
    BookDto getById(Long id);
}
