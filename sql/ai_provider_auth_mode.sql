-- 为已有 AI Provider 配置增加 Anthropic 认证方式
SET @auth_mode_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_provider'
    AND COLUMN_NAME = 'auth_mode'
);
SET @auth_mode_sql = IF(
  @auth_mode_exists = 0,
  'ALTER TABLE `ai_provider` ADD COLUMN `auth_mode` varchar(32) NOT NULL DEFAULT ''api_key'' COMMENT ''api_key | auth_token，仅 Anthropic 使用'' AFTER `api_key`',
  'SELECT 1'
);
PREPARE auth_mode_statement FROM @auth_mode_sql;
EXECUTE auth_mode_statement;
DEALLOCATE PREPARE auth_mode_statement;
