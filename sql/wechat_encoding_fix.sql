SET NAMES utf8mb4;
USE nova_mall;

-- 修复乱码版

UPDATE sys_config SET config_name = '微信公众号功能开关', remark = '是否启用微信公众号模块'
WHERE config_key = 'wechat.enabled';

UPDATE sys_config SET config_name = '微信公众号默认账号ID', remark = '为空时需前端显式传 accountId'
WHERE config_key = 'wechat.defaultAccountId';

UPDATE sys_config SET config_name = '公众号回调密文模式', remark = 'true=兼容/安全模式，false=明文模式'
WHERE config_key = 'wechat.callback.encrypt';

UPDATE sys_menu SET menu_name = '公众号', remark = '微信公众号管理目录' WHERE menu_id = 2050;
UPDATE sys_menu SET menu_name = '账号管理' WHERE menu_id = 2051;
UPDATE sys_menu SET menu_name = '推送记录' WHERE menu_id = 2052;
UPDATE sys_menu SET menu_name = '素材管理' WHERE menu_id = 2053;
UPDATE sys_menu SET menu_name = '菜单管理' WHERE menu_id = 2054;
UPDATE sys_menu SET menu_name = '自动回复' WHERE menu_id = 2055;
UPDATE sys_menu SET menu_name = '粉丝管理' WHERE menu_id = 2056;
UPDATE sys_menu SET menu_name = '消息日志' WHERE menu_id = 2057;
UPDATE sys_menu SET menu_name = '账号查询' WHERE menu_id = 2300;
UPDATE sys_menu SET menu_name = '账号新增' WHERE menu_id = 2301;
UPDATE sys_menu SET menu_name = '账号修改' WHERE menu_id = 2302;
UPDATE sys_menu SET menu_name = '账号删除' WHERE menu_id = 2303;
UPDATE sys_menu SET menu_name = '推送执行' WHERE menu_id = 2304;
UPDATE sys_menu SET menu_name = '素材删除' WHERE menu_id = 2305;
UPDATE sys_menu SET menu_name = '菜单新增' WHERE menu_id = 2306;
UPDATE sys_menu SET menu_name = '菜单修改' WHERE menu_id = 2307;
UPDATE sys_menu SET menu_name = '菜单发布' WHERE menu_id = 2308;
UPDATE sys_menu SET menu_name = '回复新增' WHERE menu_id = 2309;
UPDATE sys_menu SET menu_name = '回复修改' WHERE menu_id = 2310;
