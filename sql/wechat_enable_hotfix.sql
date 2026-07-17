SET NAMES utf8mb4;
USE nova_mall;

-- P1 功能开关默认 false 会导致线上公众号接口全部被拦截，恢复为默认启用
UPDATE sys_config SET config_value = 'true' WHERE config_key = 'wechat.enabled' AND config_value = 'false';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '微信公众号功能开关', 'wechat.enabled', 'true', 'Y', 'admin', sysdate(), '是否启用微信公众号模块（默认启用）'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.enabled');
