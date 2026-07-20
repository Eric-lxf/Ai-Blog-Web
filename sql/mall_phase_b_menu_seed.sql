SET NAMES utf8mb4;
USE nova_mall;

-- Phase B: 属性管理 / 前台类目菜单与权限（menu_id 3010–3011，按钮 3150–3163）
-- 依赖: mall_menu_seed.sql

UPDATE sys_menu SET menu_name = '后台类目' WHERE menu_id = 3001;

INSERT IGNORE INTO sys_menu VALUES
(3010, '属性管理', 3000, 6, 'attr', 'mall/admin/attr/index', '', '', 1, 0, 'C', '0', '0', 'mall:attr:list', 'edit', 'admin', sysdate(), '', NULL, ''),
(3011, '前台类目', 3000, 7, 'frontCategory', 'mall/admin/frontCategory/index', '', '', 1, 0, 'C', '0', '0', 'mall:frontCategory:list', 'tree', 'admin', sysdate(), '', NULL, ''),

(3150, '属性查询', 3010, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:attr:query', '#', 'admin', sysdate(), '', NULL, ''),
(3151, '属性新增', 3010, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:attr:add', '#', 'admin', sysdate(), '', NULL, ''),
(3152, '属性修改', 3010, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:attr:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3153, '属性删除', 3010, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:attr:remove', '#', 'admin', sysdate(), '', NULL, ''),

(3160, '前台类目查询', 3011, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:frontCategory:query', '#', 'admin', sysdate(), '', NULL, ''),
(3161, '前台类目新增', 3011, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:frontCategory:add', '#', 'admin', sysdate(), '', NULL, ''),
(3162, '前台类目修改', 3011, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:frontCategory:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3163, '前台类目删除', 3011, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:frontCategory:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (3010, 3011, 3150, 3151, 3152, 3153, 3160, 3161, 3162, 3163);
