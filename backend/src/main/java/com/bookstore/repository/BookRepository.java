package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA 仓储接口 —— 持久化 {@link Book}。
 * <p>
 * 继承自 {@code JpaRepository<Book, Long>}，自动获得
 * {@code save / findById / findAll / deleteById / count ...} 等常用方法，
 * 无需手写 SQL。下方还演示了两种自定义查询：
 *   1) <b>方法名派生查询</b>（findByCategory / findByAuthorContaining）
 *      —— 由 Spring Data 在运行时根据方法名自动翻译成 SQL；
 *   2) <b>@Query 注解</b>（searchByKeyword）
 *      —— 直接写 JPQL，适合更灵活的查询。
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /** 按分类精确匹配。 */
    List<Book> findByCategory(String category);

    /** 按作者名模糊查询（LIKE '%xxx%'）。 */
    List<Book> findByAuthorContaining(String authorKeyword);

    /** 按书名 / 作者 / 分类做全字段模糊检索，演示 @Query JPQL 写法。 */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title)    LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(b.author)   LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(b.category) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<Book> searchByKeyword(@Param("kw") String keyword);
}
