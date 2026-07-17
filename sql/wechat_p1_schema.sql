SET NAMES utf8mb4;
USE nova_mall;

INSERT IGNORE INTO sys_menu VALUES
(2060, '模块配置', 2050, 10, 'config', 'wechat/config/index', '', 'WechatConfig', 1, 0, 'C', '0', '0', 'wechat:config:query', 'system', 'admin', sysdate(), '', NULL, ''),
(2322, '回复删除', 2055, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:reply:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2323, '配置查询', 2060, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:config:query', '#', 'admin', sysdate(), '', NULL, ''),
(2324, '配置修改', 2060, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:config:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2325, '菜单同步', 2054, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:sync', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2060, 2322, 2323, 2324, 2325);
