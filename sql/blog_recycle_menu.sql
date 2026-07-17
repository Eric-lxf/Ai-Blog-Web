SET NAMES utf8mb4;
USE nova_mall;

INSERT IGNORE INTO sys_menu VALUES
(2006, '文章回收站', 2000, 6, 'article/recycle', 'blog/article/recycle', '', '', 1, 0, 'C', '0', '0', 'blog:article:recycle', 'row', 'admin', sysdate(), '', NULL, ''),
(2110, '回收站查询', 2006, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:article:recycle', '#', 'admin', sysdate(), '', NULL, ''),
(2111, '文章恢复', 2006, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:article:restore', '#', 'admin', sysdate(), '', NULL, ''),
(2112, '彻底删除', 2006, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:article:purge', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 2006), (1, 2110), (1, 2111), (1, 2112),
(3, 2006), (3, 2110), (3, 2111);
