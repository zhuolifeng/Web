package com.bookstore.service;

import com.bookstore.dto.BookDto;

import java.util.List;

/**
 * 书籍业务接口。返回值统一使用 {@link BookDto}，
 * 实现类持有底层 {@link com.bookstore.repository.BookRepository}（JPA），
 * Controller 永远不会感知到 Entity 的存在，便于未来切换为多源/异构存储。
 */
public interface BookService {

    /** 列出所有书籍 */
    List<BookDto> listAll();

    /** 按主键查单本 */
    BookDto getById(Long id);

    /** 按关键词搜索书籍（书名/作者/分类模糊匹配） */
    List<BookDto> search(String keyword);

    /** 新增书籍（管理员） */
    BookDto create(BookDto dto);

    /** 更新书籍信息（管理员） */
    BookDto update(Long id, BookDto dto);

    /** 删除书籍（管理员） */
    void delete(Long id);
}
