package com.bookstore.service.impl;

import com.bookstore.dto.BookDto;
import com.bookstore.entity.Book;
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

    @Override
    public List<BookDto> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listAll();
        }
        return bookRepository.searchByKeyword(keyword.trim()).stream()
                .map(BookDto::from)
                .toList();
    }

    @Override
    @Transactional
    public BookDto create(BookDto dto) {
        Book book = new Book();
        copyDtoToEntity(dto, book);
        if (book.getStock() == null) book.setStock(0);
        return BookDto.from(bookRepository.save(book));
    }

    @Override
    @Transactional
    public BookDto update(Long id, BookDto dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "书籍不存在: id=" + id));
        copyDtoToEntity(dto, book);
        return BookDto.from(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BusinessException(404, "书籍不存在: id=" + id);
        }
        bookRepository.deleteById(id);
    }

    /** 将 DTO 字段拷贝到 Entity（新增和更新复用） */
    private void copyDtoToEntity(BookDto dto, Book book) {
        if (dto.getTitle() != null) book.setTitle(dto.getTitle());
        if (dto.getAuthor() != null) book.setAuthor(dto.getAuthor());
        book.setCategory(dto.getCategory());
        book.setBadge(dto.getBadge());
        book.setStars(dto.getStars());
        book.setRatingNum(dto.getRatingNum());
        book.setRatingCount(dto.getRatingCount());
        if (dto.getPrice() != null) book.setPrice(dto.getPrice());
        book.setOriginalPrice(dto.getOriginalPrice());
        book.setDescription(dto.getDescription());
        book.setIntro(dto.getIntro());
        book.setAuthorBio(dto.getAuthorBio());
        book.setPublisher(dto.getPublisher());
        book.setPublishDate(dto.getPublishDate());
        book.setPages(dto.getPages());
        book.setIsbn(dto.getIsbn());
        book.setBinding(dto.getBinding());
        book.setCoverImg(dto.getCoverImg());
        book.setCoverEmoji(dto.getCoverEmoji());
        if (dto.getStock() != null) book.setStock(dto.getStock());
    }
}
