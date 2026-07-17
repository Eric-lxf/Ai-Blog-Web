SET NAMES utf8mb4;
USE nova_mall;

-- 已部署环境：修正消息中心菜单组件路径与路由名称，避免打开白屏
UPDATE sys_menu
SET component = 'blog/notification/index',
    route_name = 'BlogNotification'
WHERE menu_id = 2040;

UPDATE sys_menu
SET component = 'blog/notification/send',
    route_name = 'BlogNotificationSend'
WHERE menu_id = 2041;
