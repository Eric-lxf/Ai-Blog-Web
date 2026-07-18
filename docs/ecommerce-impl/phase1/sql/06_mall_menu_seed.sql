SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: 商城后台菜单与权限（menu_id 3000–3999，避开博客 2000+ / 公众号 2050+）
-- 组件路径按实现时 frontend/src/views/mall/admin/** 约定，可按实际路由再跑 fix 脚本调整

INSERT IGNORE INTO sys_menu VALUES
(3000, '商城', 0, 6, 'mall', NULL, '', '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), '', NULL, '商城管理目录'),
(3001, '类目管理', 3000, 1, 'category', 'mall/admin/category/index', '', '', 1, 0, 'C', '0', '0', 'mall:category:list', 'tree', 'admin', sysdate(), '', NULL, ''),
(3002, '品牌管理', 3000, 2, 'brand', 'mall/admin/brand/index', '', '', 1, 0, 'C', '0', '0', 'mall:brand:list', 'star', 'admin', sysdate(), '', NULL, ''),
(3003, '商品管理', 3000, 3, 'spu', 'mall/admin/spu/index', '', '', 1, 0, 'C', '0', '0', 'mall:spu:list', 'component', 'admin', sysdate(), '', NULL, ''),
(3004, '订单管理', 3000, 4, 'order', 'mall/admin/order/index', '', '', 1, 0, 'C', '0', '0', 'mall:order:list', 'list', 'admin', sysdate(), '', NULL, ''),
(3005, '支付单', 3000, 5, 'payment', 'mall/admin/payment/index', '', '', 1, 0, 'C', '0', '0', 'mall:payment:list', 'money', 'admin', sysdate(), '', NULL, ''),

(3100, '类目查询', 3001, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:query', '#', 'admin', sysdate(), '', NULL, ''),
(3101, '类目新增', 3001, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:add', '#', 'admin', sysdate(), '', NULL, ''),
(3102, '类目修改', 3001, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3103, '类目删除', 3001, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:category:remove', '#', 'admin', sysdate(), '', NULL, ''),

(3110, '品牌查询', 3002, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:brand:query', '#', 'admin', sysdate(), '', NULL, ''),
(3111, '品牌新增', 3002, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:brand:add', '#', 'admin', sysdate(), '', NULL, ''),
(3112, '品牌修改', 3002, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:brand:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3113, '品牌删除', 3002, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:brand:remove', '#', 'admin', sysdate(), '', NULL, ''),

(3120, '商品查询', 3003, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:spu:query', '#', 'admin', sysdate(), '', NULL, ''),
(3121, '商品新增', 3003, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:spu:add', '#', 'admin', sysdate(), '', NULL, ''),
(3122, '商品修改', 3003, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:spu:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3123, '商品删除', 3003, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:spu:remove', '#', 'admin', sysdate(), '', NULL, ''),
(3124, '商品上架', 3003, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:spu:publish', '#', 'admin', sysdate(), '', NULL, ''),

(3130, '订单查询', 3004, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:order:query', '#', 'admin', sysdate(), '', NULL, ''),
(3131, '订单发货', 3004, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:order:ship', '#', 'admin', sysdate(), '', NULL, ''),
(3132, '订单完成', 3004, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:order:complete', '#', 'admin', sysdate(), '', NULL, ''),

(3140, '支付单查询', 3005, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'mall:payment:query', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 3000 AND menu_id < 4000;

-- C 端买家角色（无后台菜单，仅用于业务鉴权标识；具体权限注解实现时再挂）
INSERT IGNORE INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, del_flag, create_by, create_time, remark)
VALUES (4, '商城买家', 'mall_customer', 4, '5', '0', '0', 'admin', sysdate(), 'C端买家角色，默认不授予后台菜单');
