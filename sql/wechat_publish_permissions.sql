-- 微信公众号发布能力：查询与删除权限
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2313, '发布查询', 2052, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:publish:query', '#', 'admin', sysdate(), '', NULL, ''),
(2314, '发布删除', 2052, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:publish:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2313, 2314);
