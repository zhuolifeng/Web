package com.bookstore.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.bookstore.cache.BookCache;
import com.bookstore.dto.BookDto;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通过 Spring Data JPA 的 {@link BookRepository} 访问 books 表，
 * 再经 {@link BookDto#from} 转换为 DTO 暴露给上层。
 * <p>
 * hw7：图书详情走 Redis 旁路缓存（{@link BookCache}），写操作提交后删除缓存；
 * 详情查询同时被 Sentinel 资源 {@value #BOOK_DETAIL_RESOURCE} 保护，异常比例过高时熔断降级。
 */
@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    /** Sentinel 资源名：图书详情查询（熔断降级规则作用于此资源） */
    public static final String BOOK_DETAIL_RESOURCE = "bookDetailQuery";

    private static final Logger log = LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookRepository bookRepository;
    private final BookCache bookCache;

    public BookServiceImpl(BookRepository bookRepository, BookCache bookCache) {
        this.bookRepository = bookRepository;
        this.bookCache = bookCache;
    }

    @Override
    public List<BookDto> listAll() {
        return bookRepository.findAll().stream()
                .map(BookDto::from)
                .toList();
    }

    /**
     * 图书详情：Redis Cache-Aside。
     * <p>
     * 不开启外层事务（NOT_SUPPORTED）：等待互斥锁的线程不应长时间占用数据库连接，
     * 真正查库时由 Repository 自己的只读事务完成。
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @SentinelResource(value = BOOK_DETAIL_RESOURCE,
            blockHandler = "getByIdBlocked",
            exceptionsToIgnore = BusinessException.class)
    public BookDto getById(Long id) {
        if (id == null || id <= 0) {
            // 非法 id 直接拦截，连缓存都不必访问（防穿透的第一道防线）
            throw new BusinessException(404, "书籍不存在: id=" + id);
        }
        return bookCache.getOrLoad(id, bookId -> bookRepository.findById(bookId).map(BookDto::from))
                .orElseThrow(() -> new BusinessException(404, "书籍不存在: id=" + id));
    }

    /** Sentinel 熔断（或资源级限流）时的降级处理 */
    public BookDto getByIdBlocked(Long id, BlockException ex) {
        log.warn("[Sentinel] 图书详情 id={} 被 {} 拦截，返回降级信息", id, ex.getClass().getSimpleName());
        throw new BusinessException(503, "图书详情服务暂时不可用（已熔断降级），请稍后重试");
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
        BookDto saved = BookDto.from(bookRepository.save(book));
        // 新 id 之前可能被当作“不存在”缓存过空对象，提交后一并清掉
        bookCache.evictAfterCommit(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public BookDto update(Long id, BookDto dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "书籍不存在: id=" + id));
        copyDtoToEntity(dto, book);
        BookDto saved = BookDto.from(bookRepository.save(book));
        bookCache.evictAfterCommit(id);
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BusinessException(404, "书籍不存在: id=" + id);
        }
        bookRepository.deleteById(id);
        bookCache.evictAfterCommit(id);
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
