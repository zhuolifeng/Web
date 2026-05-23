package com.bookstore.service;

import com.bookstore.entity.Book;

import java.util.List;

public interface BookService {

    List<Book> listAll();

    Book getById(Long id);
}
