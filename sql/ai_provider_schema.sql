-- AI 多厂商 Provider 配置（OpenAI / Claude / DeepSeek 等）
-- menu_id 2430-2439

CREATE TABLE IF NOT EXISTS `ai_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '配置名称',
  `provider_type` varchar(32) NOT NULL COMMENT 'openai_compatible | anthropic',
  `api_key` varchar(512) NOT NULL COMMENT 'API Key',
  `base_url` varchar(255) NOT NULL COMMENT 'API Base URL',
  `default_model` varchar(128) NOT NULL COMMENT '默认对话模型',
  `vision_model` varchar(128) DEFAULT NULL COMMENT '视觉模型（可选）',
  `timeout_seconds` int NOT NULL DEFAULT 300 COMMENT '超时秒数',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0停用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_provider_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型服务商配置';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'AI默认Provider', 'ai.defaultProviderId', '', 'Y', 'admin', sysdate(), '为空时使用首个启用的 Provider'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'ai.defaultProviderId');

-- 菜单：挂在 AI博客(2000) 下
INSERT IGNORE INTO sys_menu VALUES
(2430, 'AI模型配置', 2000, 9, 'ai/provider', 'blog/ai/provider/index', '', 'BlogAiProvider', 1, 0, 'C', '0', '0', 'blog:ai:provider:list', 'tool', 'admin', sysdate(), '', NULL, '多厂商 AI Key 与模型配置'),
(2431, 'Provider查询', 2430, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:ai:provider:query', '#', 'admin', sysdate(), '', NULL, ''),
(2432, 'Provider新增', 2430, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:ai:provider:add', '#', 'admin', sysdate(), '', NULL, ''),
(2433, 'Provider修改', 2430, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:ai:provider:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2434, 'Provider删除', 2430, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:ai:provider:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2435, 'Provider测试', 2430, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:ai:provider:test', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2430 AND 2435;
