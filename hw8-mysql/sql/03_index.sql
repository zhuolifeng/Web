-- =====================================================================
-- hw8：复合索引。等值条件列 category 在前、范围/排序列 price 在后（最左前缀原则）。
-- =====================================================================
USE bookstore_hw8;
CREATE INDEX idx_category_price ON books (category, price);
ANALYZE TABLE books;
SHOW INDEX FROM books;
