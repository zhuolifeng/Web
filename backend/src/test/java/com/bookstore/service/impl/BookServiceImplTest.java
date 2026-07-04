package com.bookstore.service.impl;

import com.bookstore.dto.BookDto;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookServiceImpl 单元测试。
 * 使用 Mockito 模拟 BookRepository，验证业务逻辑的正确性。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("百年孤独");
        sampleBook.setAuthor("加西亚·马尔克斯");
        sampleBook.setCategory("文学小说");
        sampleBook.setIsbn("978-7-5442-7115-3");
        sampleBook.setPrice("¥49.00");
        sampleBook.setStock(50);
    }

    @Test
    @DisplayName("listAll - 返回所有书籍的DTO列表")
    void testListAll() {
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("三体");
        book2.setAuthor("刘慈欣");
        book2.setStock(60);

        when(bookRepository.findAll()).thenReturn(Arrays.asList(sampleBook, book2));

        List<BookDto> result = bookService.listAll();

        assertEquals(2, result.size());
        assertEquals("百年孤独", result.get(0).getTitle());
        assertEquals("三体", result.get(1).getTitle());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getById - 书籍存在时返回DTO")
    void testGetByIdFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        BookDto result = bookService.getById(1L);

        assertNotNull(result);
        assertEquals("百年孤独", result.getTitle());
        assertEquals("加西亚·马尔克斯", result.getAuthor());
    }

    @Test
    @DisplayName("getById - 书籍不存在时抛出异常")
    void testGetByIdNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookService.getById(99L));
        assertEquals(404, exception.getCode());
    }

    @Test
    @DisplayName("search - 空关键词返回全部书籍")
    void testSearchWithEmptyKeyword() {
        when(bookRepository.findAll()).thenReturn(Collections.singletonList(sampleBook));

        List<BookDto> result = bookService.search("");

        assertEquals(1, result.size());
        verify(bookRepository).findAll();
        verify(bookRepository, never()).searchByKeyword(anyString());
    }

    @Test
    @DisplayName("search - 有关键词时调用模糊搜索")
    void testSearchWithKeyword() {
        when(bookRepository.searchByKeyword("百年")).thenReturn(Collections.singletonList(sampleBook));

        List<BookDto> result = bookService.search("百年");

        assertEquals(1, result.size());
        assertEquals("百年孤独", result.get(0).getTitle());
        verify(bookRepository).searchByKeyword("百年");
    }

    @Test
    @DisplayName("create - 新增书籍并返回DTO")
    void testCreate() {
        BookDto dto = new BookDto();
        dto.setTitle("新书");
        dto.setAuthor("新作者");
        dto.setPrice("¥39.00");
        dto.setStock(100);

        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        BookDto result = bookService.create(dto);

        assertNotNull(result);
        assertEquals("新书", result.getTitle());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("update - 更新已有书籍信息")
    void testUpdate() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook);

        BookDto updateDto = new BookDto();
        updateDto.setTitle("百年孤独（修订版）");
        updateDto.setAuthor("加西亚·马尔克斯");
        updateDto.setStock(30);

        BookDto result = bookService.update(1L, updateDto);

        assertNotNull(result);
        verify(bookRepository).findById(1L);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("update - 书籍不存在时抛出异常")
    void testUpdateNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        BookDto dto = new BookDto();
        dto.setTitle("不存在");

        assertThrows(BusinessException.class, () -> bookService.update(99L, dto));
    }

    @Test
    @DisplayName("delete - 删除已有书籍")
    void testDelete() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete - 删除不存在的书籍时抛出异常")
    void testDeleteNotFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> bookService.delete(99L));
        verify(bookRepository, never()).deleteById(anyLong());
    }
}
