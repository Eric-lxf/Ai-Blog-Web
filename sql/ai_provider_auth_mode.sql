-- 为已有 AI Provider 配置增加 Anthropic 认证方式
ALTER TABLE `ai_provider`
  ADD COLUMN IF NOT EXISTS `auth_mode` varchar(32) NOT NULL DEFAULT 'api_key'
  COMMENT 'api_key | auth_token，仅 Anthropic 使用' AFTER `api_key`;
