-- 微信公众号菜单：新增查询与删除权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2311, '菜单查询', 2054, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:query', '#', 'admin', sysdate(), '', NULL, ''),
(2312, '菜单删除', 2054, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2311, 2312);
