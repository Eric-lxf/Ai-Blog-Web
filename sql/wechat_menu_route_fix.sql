SET NAMES utf8mb4;
USE ai_blog;

-- Fix route_name/path conflicts (e.g. wechat "menu" vs system/menu -> name "Menu").
-- Safe to rerun.

UPDATE sys_menu SET route_name = 'SystemMenu' WHERE menu_id = 102;

UPDATE sys_menu SET route_name = 'WechatAdmin' WHERE menu_id = 2050;
UPDATE sys_menu SET path = 'account', component = 'wechat/account/index', route_name = 'WechatAccount' WHERE menu_id = 2051;
UPDATE sys_menu SET path = 'publish', component = 'wechat/publish/index', route_name = 'WechatPublish' WHERE menu_id = 2052;
UPDATE sys_menu SET path = 'material', component = 'wechat/material/index', route_name = 'WechatMaterial' WHERE menu_id = 2053;
UPDATE sys_menu SET path = 'wx-menu', component = 'wechat/menu/index', route_name = 'WechatMenu' WHERE menu_id = 2054;
UPDATE sys_menu SET path = 'reply', component = 'wechat/reply/index', route_name = 'WechatReply' WHERE menu_id = 2055;
UPDATE sys_menu SET path = 'fans', component = 'wechat/fans/index', route_name = 'WechatFans' WHERE menu_id = 2056;
UPDATE sys_menu SET path = 'message-log', component = 'wechat/message/index', route_name = 'WechatMessageLog' WHERE menu_id = 2057;
