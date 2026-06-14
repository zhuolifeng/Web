package com.bookstore.service.impl;

import com.bookstore.dto.BookDto;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通过 Spring Data JPA 的 {@link BookRepository} 访问 books 表，
 * 再经 {@link BookDto#from} 转换为 DTO 暴露给上层。
 */
@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<BookDto> listAll() {
        return bookRepository.findAll().stream()
                .map(BookDto::from)
                .toList();
    }

    @Override
    public BookDto getById(Long id) {
        return bookRepository.findById(id)
                .map(BookDto::from)
                .orElseThrow(() -> new BusinessException(404, "书籍不存在: id=" + id));
    }
}
