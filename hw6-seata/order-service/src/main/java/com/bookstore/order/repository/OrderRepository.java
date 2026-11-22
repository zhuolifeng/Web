package com.bookstore.order.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/** 订单库（bookstore_order）数据访问。 */
@Repository
public class OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(OrderRepository.class);

    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 订单入库。
     *
     * @param simulateError true 时在 INSERT 执行之后模拟抛出 RuntimeException（作业核心考察点）
     */
    public long insert(String orderNo, Long userId, Long bookId, int quantity,
                       BigDecimal unitPrice, BigDecimal totalAmount, boolean simulateError) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orders (order_no, user_id, book_id, quantity, unit_price, total_amount, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 'CREATED')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderNo);
            ps.setLong(2, userId);
            ps.setLong(3, bookId);
            ps.setInt(4, quantity);
            ps.setBigDecimal(5, unitPrice);
            ps.setBigDecimal(6, totalAmount);
            return ps;
        }, keyHolder);
        long orderId = keyHolder.getKey().longValue();
        log.info("[订单中心] 订单 INSERT 已执行 orderId={} orderNo={}", orderId, orderNo);

        if (simulateError) {
            log.error("[订单中心] >>> 模拟订单入库异常：即将抛出 RuntimeException（orderNo={}）", orderNo);
            throw new RuntimeException("模拟订单入库异常：订单写库失败（orderNo=" + orderNo + "）");
        }
        return orderId;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("SELECT * FROM orders ORDER BY id");
    }
}
