SET NAMES utf8mb4;
USE ai_blog;

-- 1. 新增与「AI博客」同级的「博客」目录
INSERT IGNORE INTO sys_menu VALUES
(2030, '博客', 0, 6, 'blog-ops', NULL, '', '', 1, 0, 'M', '0', '0', '', 'list', 'admin', sysdate(), '', NULL, '博客运营'),
(2031, '博客列表', 2030, 1, 'list', 'blog/list/index', '', '', 1, 0, 'C', '0', '0', 'blog:article:list', 'documentation', 'admin', sysdate(), '', NULL, '');

-- 2. 评论菜单迁至「博客」下，并修复 path（避免 comment 与 comment/xxx 路由冲突白屏）
UPDATE sys_menu SET parent_id = 2030, path = 'comment-manage', order_num = 2
WHERE menu_id = 2010;
UPDATE sys_menu SET parent_id = 2030, path = 'comment-report', order_num = 3
WHERE menu_id = 2011;
UPDATE sys_menu SET parent_id = 2030, path = 'comment-sensitive', order_num = 4
WHERE menu_id = 2012;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2030), (1, 2031),
(3, 2030), (3, 2031),
(3, 2010), (3, 2011), (3, 2012),
(3, 2200), (3, 2201), (3, 2202), (3, 2203), (3, 2204),
(3, 2210), (3, 2211), (3, 2212), (3, 2213);
