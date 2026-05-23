package com.bookstore.service.impl;

import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> listAll() {
        return bookRepository.findAll();
    }

    @Override
    public Book getById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "书籍不存在: id=" + id));
    }
}
