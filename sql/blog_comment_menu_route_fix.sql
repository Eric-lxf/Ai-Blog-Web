SET NAMES utf8mb4;
USE nova_mall;

-- 修正博客运营子菜单 route_name，与 Vue defineOptions.name 一致，避免 keep-alive 与路由切换异常
UPDATE sys_menu SET route_name = 'BlogList' WHERE menu_id = 2031;
UPDATE sys_menu SET route_name = 'BlogCommentManage' WHERE menu_id = 2010;
UPDATE sys_menu SET route_name = 'BlogCommentReport' WHERE menu_id = 2011;
UPDATE sys_menu SET route_name = 'BlogCommentSensitive' WHERE menu_id = 2012;
UPDATE sys_menu SET route_name = 'BlogNotification' WHERE menu_id = 2040;
UPDATE sys_menu SET route_name = 'BlogNotificationSend' WHERE menu_id = 2041;
