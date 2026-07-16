-- 账单对齐微信支付交易明细字段（幂等）
-- 交易单号 / 交易时间 / 交易类型 / 收支出 / 商户单号

SET @dbname = DATABASE();

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `blog_bill` ADD COLUMN `trade_no` varchar(64) DEFAULT NULL COMMENT ''交易单号'' AFTER `id`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'blog_bill' AND COLUMN_NAME = 'trade_no'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `blog_bill` ADD COLUMN `trade_time` datetime DEFAULT NULL COMMENT ''交易时间'' AFTER `bill_date`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'blog_bill' AND COLUMN_NAME = 'trade_time'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `blog_bill` ADD COLUMN `trade_type` varchar(32) DEFAULT NULL COMMENT ''交易类型：商户消费/转账等'' AFTER `trade_time`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'blog_bill' AND COLUMN_NAME = 'trade_type'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `blog_bill` ADD COLUMN `direction` varchar(16) DEFAULT ''支出'' COMMENT ''收/支/其他'' AFTER `trade_type`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'blog_bill' AND COLUMN_NAME = 'direction'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `blog_bill` ADD COLUMN `merchant_order_no` varchar(64) DEFAULT NULL COMMENT ''商户单号'' AFTER `payment_method`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'blog_bill' AND COLUMN_NAME = 'merchant_order_no'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `blog_bill` ADD INDEX `idx_user_trade_no` (`user_id`, `trade_no`)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'blog_bill' AND INDEX_NAME = 'idx_user_trade_no'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE `blog_bill` MODIFY COLUMN `payment_method` varchar(100) DEFAULT NULL COMMENT '交易方式';
ALTER TABLE `blog_bill` MODIFY COLUMN `merchant` varchar(200) DEFAULT NULL COMMENT '交易对方/商户名称';
