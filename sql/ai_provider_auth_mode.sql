-- 为已存在的 ai_provider 表增加 Claude 鉴权模式列
-- api_key: x-api-key（控制台普通 Key）
-- auth_token: Authorization Bearer（等同 ANTHROPIC_AUTH_TOKEN）

ALTER TABLE `ai_provider`
  ADD COLUMN `auth_mode` varchar(32) NOT NULL DEFAULT 'api_key'
    COMMENT 'api_key(x-api-key) | auth_token(Bearer/ANTHROPIC_AUTH_TOKEN)'
    AFTER `provider_type`;
