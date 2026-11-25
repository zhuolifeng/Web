-- =====================================================================
-- hw8：EXPLAIN 分析（由 scripts/run_all.sh 逐条执行并记录输出，可单独在 mysql 客户端运行）
-- 查询：某类别下价格在 50~100 之间的书
-- =====================================================================
USE bookstore_hw8;

-- [Q1] 主查询：SELECT * → 预期 type=range, key=idx_category_price, Extra=Using index condition
EXPLAIN SELECT * FROM books WHERE category = '科幻' AND price BETWEEN 50 AND 100;

-- [Q2] 覆盖索引：只取索引中已有的列（二级索引叶子节点自带主键 id）→ Extra=Using where; Using index
EXPLAIN SELECT id, category, price FROM books WHERE category = '科幻' AND price BETWEEN 50 AND 100;

-- [Q3] 按价格排序分页：索引本身按 (category, price) 有序 → 无 Using filesort
EXPLAIN SELECT * FROM books WHERE category = '科幻' AND price BETWEEN 50 AND 100 ORDER BY price LIMIT 20;

-- [Q4] 反例：跳过最左列 category，只按 price 过滤 → 索引无法用于范围定位，type=ALL
EXPLAIN SELECT * FROM books WHERE price BETWEEN 50 AND 100;

-- [Q5] 对照：强制不用索引（模拟未建索引）→ type=ALL 全表扫描约 10 万行
EXPLAIN SELECT * FROM books IGNORE INDEX (idx_category_price) WHERE category = '科幻' AND price BETWEEN 50 AND 100;

-- [Q6] 实际执行耗时对比
EXPLAIN ANALYZE SELECT * FROM books WHERE category = '科幻' AND price BETWEEN 50 AND 100;
EXPLAIN ANALYZE SELECT * FROM books IGNORE INDEX (idx_category_price) WHERE category = '科幻' AND price BETWEEN 50 AND 100;

-- [Q7] 反范式收益：“我的订单”列表
--  范式化写法：需 JOIN users 取昵称 + JOIN order_items 聚合件数与书名
EXPLAIN SELECT o.order_no, u.nickname, SUM(i.quantity) AS item_count,
               GROUP_CONCAT(i.book_title) AS titles, o.total_amount, o.created_at
FROM orders o JOIN users u ON u.id = o.user_id JOIN order_items i ON i.order_id = o.id
WHERE o.user_id = 42 GROUP BY o.id ORDER BY o.created_at DESC LIMIT 10;
--  反范式写法：单表 + 索引 (user_id, created_at)，无 JOIN、无 GROUP BY、无 filesort
EXPLAIN SELECT order_no, user_nickname, item_count, book_snapshot, total_amount, created_at
FROM orders WHERE user_id = 42 ORDER BY created_at DESC LIMIT 10;
