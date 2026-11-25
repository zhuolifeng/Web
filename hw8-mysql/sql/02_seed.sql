-- =====================================================================
-- hw8：模拟数据。图书 100,000 条（递归 CTE 一条 INSERT ... SELECT 生成，数秒完成）；
--      另生成 1,000 个用户、10,000 个订单用于对比反范式查询。
--      使用 RAND(seed) / 取模保证每次生成的数据一致、可复现。
-- =====================================================================
USE bookstore_hw8;
SET SESSION cte_max_recursion_depth = 200000;

-- ---------------------------------------------------------------- 图书 10 万条
INSERT INTO books (isbn, title, author, category, publisher, publish_date,
                   price, original_price, stock, sales, rating, cover_img, description)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT CONCAT('978-7-', LPAD(n, 8, '0')),
       CONCAT(ELT(1 + n % 10, '文学小说','科幻','历史','经济管理','计算机','少儿','艺术','哲学','心理学','旅行'),
              '·模拟图书 ', n),
       CONCAT('作者', 1 + n % 5000),
       ELT(1 + n % 10, '文学小说','科幻','历史','经济管理','计算机','少儿','艺术','哲学','心理学','旅行'),
       ELT(1 + n % 6, '人民文学出版社','译林出版社','中信出版社','机械工业出版社','重庆出版社','三联书店'),
       DATE_ADD('2000-01-01', INTERVAL n % 9000 DAY),
       ROUND(10 + RAND(n) * 190, 2)          AS price,           -- 10.00 ~ 200.00 均匀分布
       ROUND(10 + RAND(n) * 190 * 1.25, 2)   AS original_price,
       n % 500,
       (n * 7919) % 100000,
       ROUND(3 + (n % 21) / 10, 1),
       CONCAT('/images/books/', n % 100, '.jpg'),
       CONCAT('这是第 ', n, ' 本模拟图书的简介。')
FROM seq;

-- ---------------------------------------------------------------- 用户 1,000 个
INSERT INTO users (username, password_hash, nickname, email, phone)
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
SELECT CONCAT('user', n),
       '$2a$10$jp9oDxaKUagip95yRemW.eVp4arjAfCUkd1jYn7LzKrk.0lU6B/YG',
       CONCAT('读者', LPAD(n, 4, '0')),
       CONCAT('user', n, '@example.com'),
       CONCAT('138', LPAD(n, 8, '0'))
FROM seq;

-- ---------------------------------------------------------------- 订单 10,000 个，每单 2 种书
-- 先写详情（快照取自 books 当前值），再按详情汇总写主表的冗余字段
CREATE TEMPORARY TABLE tmp_order_seq AS
WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10000)
SELECT n, 1 + (n * 37) % 1000 AS user_id FROM seq;

INSERT INTO orders (id, order_no, user_id, user_nickname, item_count, book_snapshot,
                    total_amount, status, receiver, phone, address, created_at)
SELECT s.n, CONCAT('2026', LPAD(s.n, 16, '0')), s.user_id, u.nickname, 0, JSON_ARRAY(),
       0, ELT(1 + s.n % 4, 'PAID','SHIPPED','COMPLETED','CANCELLED'),
       u.nickname, u.phone, CONCAT('上海市闵行区东川路 ', s.n % 800, ' 号'),
       DATE_ADD('2026-01-01', INTERVAL s.n * 25 MINUTE)
FROM tmp_order_seq s JOIN users u ON u.id = s.user_id;

INSERT INTO order_items (order_id, book_id, book_title, book_author, book_isbn, cover_img,
                         unit_price, quantity, subtotal)
SELECT s.n, b.id, b.title, b.author, b.isbn, b.cover_img, b.price, q.qty, b.price * q.qty
FROM tmp_order_seq s
JOIN (SELECT 0 AS k, 1 AS qty UNION ALL SELECT 1, 2) q
JOIN books b ON b.id = 1 + (s.n * 131 + q.k * 49999) % 100000;

UPDATE orders o
JOIN (SELECT order_id,
             SUM(quantity) AS cnt,
             SUM(subtotal) AS amount,
             JSON_ARRAYAGG(JSON_OBJECT('bookId', book_id, 'title', book_title, 'cover', cover_img,
                                       'price', unit_price, 'qty', quantity)) AS snap
      FROM order_items GROUP BY order_id) t ON t.order_id = o.id
SET o.item_count = t.cnt, o.total_amount = t.amount, o.book_snapshot = t.snap;

DROP TEMPORARY TABLE tmp_order_seq;
ANALYZE TABLE users, books, orders, order_items;
