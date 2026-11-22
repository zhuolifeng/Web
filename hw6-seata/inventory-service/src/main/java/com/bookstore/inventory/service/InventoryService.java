package com.bookstore.inventory.service;

import com.bookstore.inventory.dto.DeductResult;
import org.apache.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final JdbcTemplate jdbc;

    public InventoryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 扣减库存（分支事务）。
     * <p>
     * 本地事务提交前，Seata DataSourceProxy 会：解析 UPDATE → 查询前镜像 → 执行 → 查询后镜像
     * → 写 undo_log → 向 TC 注册分支并获取全局锁。本地事务随即提交（一阶段），
     * 若全局事务最终回滚，TC 通知本服务按 undo_log 的前镜像把库存改回去（二阶段）。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeductResult deduct(Long bookId, int quantity) {
        if (bookId == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bookId/quantity 不合法");
        }
        String xid = RootContext.getXID();
        log.info("[库存中心] 开始扣减库存 XID={} bookId={} quantity={}", xid, bookId, quantity);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT title, price, stock FROM inventory WHERE book_id = ?", bookId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "书籍不存在: " + bookId);
        }
        Map<String, Object> row = rows.get(0);

        int updated = jdbc.update(
                "UPDATE inventory SET stock = stock - ? WHERE book_id = ? AND stock >= ?",
                quantity, bookId, quantity);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "库存不足: bookId=" + bookId + ", 当前库存=" + row.get("stock"));
        }

        Integer remaining = jdbc.queryForObject(
                "SELECT stock FROM inventory WHERE book_id = ?", Integer.class, bookId);
        log.info("[库存中心] 扣减成功（一阶段已提交，undo_log 已写入） XID={} bookId={} 剩余库存={}",
                xid, bookId, remaining);
        return new DeductResult(bookId, (String) row.get("title"), (BigDecimal) row.get("price"),
                quantity, remaining);
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.queryForList("SELECT book_id, title, price, stock, updated_at FROM inventory ORDER BY book_id");
    }
}
