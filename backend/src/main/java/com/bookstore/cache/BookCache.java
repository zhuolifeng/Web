package com.bookstore.cache;

import com.bookstore.dto.BookDto;
import com.bookstore.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * hw7：图书详情的 Redis 缓存（Cache-Aside 旁路缓存）。
 *
 * <pre>
 * 读：Redis 命中 → 直接返回
 *     未命中      → SET NX 抢互斥锁 → 双重检查 → 查库 → 回写缓存 → 释放锁   （防缓存击穿）
 *                    抢锁失败的线程自旋等待，期间反复读缓存，拿到别人重建的结果即返回
 *     库里也没有  → 缓存空对象 "__NULL__"（短 TTL），后续同 id 请求不再打到数据库  （防缓存穿透）
 *     正常 TTL 叠加随机抖动，避免大量 key 同时过期                               （防缓存雪崩）
 * 写：先更新数据库，事务提交后删除缓存（下一次读时重建）
 * </pre>
 *
 * Redis 不可用时自动降级为直接查库，保证缓存故障不影响业务可用性。
 */
@Component
public class BookCache {

    private static final Logger log = LoggerFactory.getLogger(BookCache.class);

    public static final String KEY_PREFIX = "bookstore:book:detail:";
    public static final String LOCK_PREFIX = "bookstore:lock:book:";
    /** 空对象占位：表示“数据库中不存在该书” */
    public static final String NULL_VALUE = "__NULL__";

    /** 只删除自己持有的锁（比较 token 再 DEL，保证原子性，避免误删别人的锁） */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else return 0 end", Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private final long ttlSeconds;
    private final long ttlJitterSeconds;
    private final long nullTtlSeconds;
    private final long lockTtlSeconds;
    private final long lockRetryIntervalMs;
    private final int lockMaxRetries;

    // 统计数据，便于验证缓存行为
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong nullHits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong dbLoads = new AtomicLong();
    private final AtomicLong lockWaits = new AtomicLong();
    private final AtomicLong redisFallbacks = new AtomicLong();

    public BookCache(StringRedisTemplate redis,
                     ObjectMapper objectMapper,
                     @Value("${bookstore.cache.book.ttl-seconds:1800}") long ttlSeconds,
                     @Value("${bookstore.cache.book.ttl-jitter-seconds:300}") long ttlJitterSeconds,
                     @Value("${bookstore.cache.book.null-ttl-seconds:120}") long nullTtlSeconds,
                     @Value("${bookstore.cache.book.lock-ttl-seconds:10}") long lockTtlSeconds,
                     @Value("${bookstore.cache.book.lock-retry-interval-ms:50}") long lockRetryIntervalMs,
                     @Value("${bookstore.cache.book.lock-max-retries:60}") int lockMaxRetries) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
        this.ttlJitterSeconds = ttlJitterSeconds;
        this.nullTtlSeconds = nullTtlSeconds;
        this.lockTtlSeconds = lockTtlSeconds;
        this.lockRetryIntervalMs = lockRetryIntervalMs;
        this.lockMaxRetries = lockMaxRetries;
    }

    public static String key(Long id) {
        return KEY_PREFIX + id;
    }

    public static String lockKey(Long id) {
        return LOCK_PREFIX + id;
    }

    /**
     * 查询图书详情（Cache-Aside + 空对象 + 互斥锁重建）。
     *
     * @param id       图书 id
     * @param dbLoader 回源查库函数
     * @return 图书；数据库中不存在时返回 {@link Optional#empty()}
     */
    public Optional<BookDto> getOrLoad(Long id, Function<Long, Optional<BookDto>> dbLoader) {
        try {
            return doGetOrLoad(id, dbLoader);
        } catch (RedisUnavailableException e) {
            // 缓存层故障：降级为直接查库，不影响可用性（限流规则仍然保护数据库）
            redisFallbacks.incrementAndGet();
            log.warn("[BookCache] Redis 不可用，降级直接查库 id={}：{}", id, e.getMessage());
            dbLoads.incrementAndGet();
            return dbLoader.apply(id);
        }
    }

    private Optional<BookDto> doGetOrLoad(Long id, Function<Long, Optional<BookDto>> dbLoader) {
        String key = key(id);

        // 1. 先查缓存
        CacheHit hit = read(key);
        if (hit != null) {
            return hit.value();
        }
        misses.incrementAndGet();

        // 2. 未命中：抢互斥锁，只允许一个线程查库重建缓存
        String lockKey = lockKey(id);
        String token = UUID.randomUUID().toString();
        for (int attempt = 0; attempt <= lockMaxRetries; attempt++) {
            if (tryLock(lockKey, token)) {
                try {
                    // 双重检查：拿到锁之前可能已有其他线程完成了重建
                    hit = read(key);
                    if (hit != null) {
                        return hit.value();
                    }
                    dbLoads.incrementAndGet();
                    log.info("[BookCache] 缓存未命中，获得互斥锁，查库重建缓存 id={}", id);
                    Optional<BookDto> loaded = dbLoader.apply(id);
                    write(key, loaded.orElse(null));
                    return loaded;
                } finally {
                    unlock(lockKey, token);
                }
            }
            // 3. 没抢到锁：短暂休眠后再读缓存，等待持锁线程重建完成
            if (attempt == 0) {
                lockWaits.incrementAndGet();
            }
            sleep(lockRetryIntervalMs);
            hit = read(key);
            if (hit != null) {
                return hit.value();
            }
        }
        log.warn("[BookCache] 等待缓存重建超时 id={}", id);
        throw new BusinessException(503, "系统繁忙，请稍后重试");
    }

    /** 写操作：事务提交后删除缓存（Cache-Aside 的“先写库、再删缓存”） */
    public void evictAfterCommit(Long id) {
        if (id == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(id);
                }
            });
        } else {
            evict(id);
        }
    }

    public void evict(Long id) {
        try {
            redis.delete(key(id));
            log.info("[BookCache] 删除缓存 id={}", id);
        } catch (RuntimeException e) {
            log.warn("[BookCache] 删除缓存失败 id={}：{}", id, e.getMessage());
        }
    }

    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("hits", hits.get());
        m.put("nullHits", nullHits.get());
        m.put("misses", misses.get());
        m.put("dbLoads", dbLoads.get());
        m.put("lockWaits", lockWaits.get());
        m.put("redisFallbacks", redisFallbacks.get());
        return m;
    }

    public void resetStats() {
        for (AtomicLong c : List.of(hits, nullHits, misses, dbLoads, lockWaits, redisFallbacks)) {
            c.set(0);
        }
    }

    // ------------------------------------------------------------------ 内部实现

    /** 缓存命中结果；value 为空表示命中了“空对象” */
    private record CacheHit(Optional<BookDto> value) {}

    /** @return 命中时返回 CacheHit，未命中返回 null */
    private CacheHit read(String key) {
        String json;
        try {
            json = redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            throw new RedisUnavailableException(e);
        }
        if (json == null) {
            return null;
        }
        if (NULL_VALUE.equals(json)) {
            nullHits.incrementAndGet();
            return new CacheHit(Optional.empty());
        }
        try {
            BookDto dto = objectMapper.readValue(json, BookDto.class);
            hits.incrementAndGet();
            return new CacheHit(Optional.of(dto));
        } catch (JsonProcessingException e) {
            // 脏数据：删除后按未命中处理
            log.warn("[BookCache] 缓存反序列化失败，删除 key={}", key);
            redis.delete(key);
            return null;
        }
    }

    private void write(String key, BookDto dto) {
        try {
            if (dto == null) {
                redis.opsForValue().set(key, NULL_VALUE, Duration.ofSeconds(nullTtlSeconds));
            } else {
                long jitter = ttlJitterSeconds > 0 ? ThreadLocalRandom.current().nextLong(ttlJitterSeconds + 1) : 0;
                redis.opsForValue().set(key, objectMapper.writeValueAsString(dto),
                        Duration.ofSeconds(ttlSeconds + jitter));
            }
        } catch (JsonProcessingException e) {
            log.warn("[BookCache] 序列化失败 key={}：{}", key, e.getMessage());
        } catch (RuntimeException e) {
            // 回写失败不影响本次返回结果
            log.warn("[BookCache] 回写缓存失败 key={}：{}", key, e.getMessage());
        }
    }

    /** SET key token NX EX lockTtl —— 带过期时间，防止持锁线程崩溃导致死锁 */
    private boolean tryLock(String lockKey, String token) {
        try {
            return Boolean.TRUE.equals(
                    redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(lockTtlSeconds)));
        } catch (RuntimeException e) {
            throw new RedisUnavailableException(e);
        }
    }

    private void unlock(String lockKey, String token) {
        try {
            redis.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
        } catch (RuntimeException e) {
            log.warn("[BookCache] 释放锁失败 {}（锁会在 {}s 后自动过期）", lockKey, lockTtlSeconds);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "请求被中断");
        }
    }

    private static class RedisUnavailableException extends RuntimeException {
        RedisUnavailableException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
