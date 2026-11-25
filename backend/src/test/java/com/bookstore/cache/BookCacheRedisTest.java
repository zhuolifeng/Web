package com.bookstore.cache;

import com.bookstore.dto.BookDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * hw7：BookCache 针对真实 Redis 的测试（本机 localhost:6379 未启动 Redis 时自动跳过）。
 * 使用独立的测试 id 段，测试前后清理相关 key，不影响业务数据。
 */
class BookCacheRedisTest {

    private static final long HOT_ID = 990_001L;
    private static final long MISSING_ID = 990_002L;

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate redis;
    private static boolean redisUp;
    private BookCache cache;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        factory.afterPropertiesSet();
        redis = new StringRedisTemplate(factory);
        try {
            redisUp = "PONG".equals(factory.getConnection().ping());
        } catch (Exception e) {
            redisUp = false;
        }
        assumeTrue(redisUp, "本机 Redis 未启动，跳过");
    }

    @AfterAll
    static void close() {
        if (redisUp) cleanup();
        if (factory != null) factory.destroy();
    }

    @BeforeEach
    void setUp() {
        cleanup();
        // TTL: 60s(+0~5s)，空对象 30s，锁 5s，等待间隔 20ms，最多 250 次
        cache = new BookCache(redis, new ObjectMapper(), 60, 5, 30, 5, 20, 250);
    }

    private static void cleanup() {
        for (long id : new long[]{HOT_ID, MISSING_ID}) {
            redis.delete(List.of(BookCache.key(id), BookCache.lockKey(id)));
        }
    }

    private static BookDto book(long id) {
        BookDto dto = new BookDto();
        dto.setId(id);
        dto.setTitle("热点书-" + id);
        dto.setStock(100);
        return dto;
    }

    @Test
    @DisplayName("Cache-Aside：首次查库并回写，之后命中缓存")
    void cacheAside() {
        AtomicInteger dbCalls = new AtomicInteger();
        Function<Long, Optional<BookDto>> loader = id -> {
            dbCalls.incrementAndGet();
            return Optional.of(book(id));
        };

        assertEquals("热点书-" + HOT_ID, cache.getOrLoad(HOT_ID, loader).orElseThrow().getTitle());
        assertEquals("热点书-" + HOT_ID, cache.getOrLoad(HOT_ID, loader).orElseThrow().getTitle());
        assertEquals(1, dbCalls.get());
        Long ttl = redis.getExpire(BookCache.key(HOT_ID));
        assertTrue(ttl != null && ttl > 0 && ttl <= 65, "应设置带抖动的过期时间, ttl=" + ttl);

        // 写操作删除缓存后，下一次读重新查库
        cache.evict(HOT_ID);
        cache.getOrLoad(HOT_ID, loader);
        assertEquals(2, dbCalls.get());
    }

    @Test
    @DisplayName("防穿透：不存在的 id 缓存空对象，重复请求不再查库")
    void penetration() {
        AtomicInteger dbCalls = new AtomicInteger();
        Function<Long, Optional<BookDto>> loader = id -> {
            dbCalls.incrementAndGet();
            return Optional.empty();
        };

        for (int i = 0; i < 20; i++) {
            assertTrue(cache.getOrLoad(MISSING_ID, loader).isEmpty());
        }
        assertEquals(1, dbCalls.get());
        assertEquals(BookCache.NULL_VALUE, redis.opsForValue().get(BookCache.key(MISSING_ID)));
        assertEquals(19, cache.stats().get("nullHits"));
    }

    @Test
    @DisplayName("防击穿：100 个线程并发请求冷 key，只有 1 个线程查库重建")
    void breakdown() throws Exception {
        int threads = 100;
        AtomicInteger dbCalls = new AtomicInteger();
        Function<Long, Optional<BookDto>> slowLoader = id -> {
            dbCalls.incrementAndGet();
            try {
                Thread.sleep(300); // 模拟慢查询，放大并发窗口
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(book(id));
        };

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Optional<BookDto>>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return cache.getOrLoad(HOT_ID, slowLoader);
            }));
        }
        start.countDown();
        for (Future<Optional<BookDto>> f : futures) {
            assertEquals(HOT_ID, f.get().orElseThrow().getId());
        }
        pool.shutdown();

        System.out.printf("[防击穿] 并发线程=%d, 全部拿到正确结果, 实际查库次数=%d, 等锁线程数=%d, 统计=%s%n",
                threads, dbCalls.get(), cache.stats().get("lockWaits"), cache.stats());
        assertEquals(1, dbCalls.get(), "缓存重建期间只允许一个线程查库");
        assertTrue(cache.stats().get("lockWaits") > 0);
        assertFalse(Boolean.TRUE.equals(redis.hasKey(BookCache.lockKey(HOT_ID))), "锁应被释放");
    }
}
