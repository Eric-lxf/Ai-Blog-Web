SET NAMES utf8mb4;
USE ai_blog;

CREATE TABLE IF NOT EXISTS `wx_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '账号名称',
  `app_id` varchar(64) NOT NULL COMMENT '微信公众号appId',
  `app_secret` varchar(128) NOT NULL COMMENT '微信公众号appSecret',
  `token` varchar(64) NOT NULL COMMENT '回调token',
  `aes_key` varchar(64) DEFAULT NULL COMMENT '消息加解密key',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信公众号账号';

CREATE TABLE IF NOT EXISTS `wx_material` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `thumb_media_id` varchar(128) DEFAULT NULL,
  `author` varchar(100) DEFAULT NULL,
  `digest` varchar(500) DEFAULT NULL,
  `content` longtext NOT NULL,
  `content_source_url` varchar(500) DEFAULT NULL,
  `media_id` varchar(128) DEFAULT NULL COMMENT '草稿/素材media_id',
  `url` varchar(500) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待上传 1草稿成功 2失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_update` (`account_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号图文素材';

CREATE TABLE IF NOT EXISTS `wx_publish_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `article_id` bigint NOT NULL COMMENT '博客文章ID',
  `material_id` bigint DEFAULT NULL,
  `publish_mode` varchar(50) NOT NULL COMMENT 'draft / draft_and_publish',
  `msg_id` varchar(128) DEFAULT NULL COMMENT '发布任务号',
  `response_body` longtext DEFAULT NULL COMMENT '微信返回原文',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1草稿成功 2发布中 3已发布 4失败',
  `error_message` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_status_time` (`account_id`, `status`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号推送记录';

CREATE TABLE IF NOT EXISTS `wx_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `menu_json` longtext NOT NULL COMMENT '菜单JSON',
  `is_published` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_update` (`account_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号菜单配置';

CREATE TABLE IF NOT EXISTS `wx_auto_reply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `reply_type` varchar(20) NOT NULL COMMENT 'keyword/default/subscribe',
  `keyword` varchar(100) DEFAULT NULL,
  `content` varchar(1000) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT 1,
  `match_type` tinyint NOT NULL DEFAULT 1 COMMENT '1包含匹配 2全等匹配',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_type` (`account_id`, `reply_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号自动回复';

CREATE TABLE IF NOT EXISTS `wx_fans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `open_id` varchar(64) NOT NULL,
  `union_id` varchar(64) DEFAULT NULL,
  `nickname` varchar(255) DEFAULT NULL,
  `subscribe_status` tinyint NOT NULL DEFAULT 1,
  `subscribe_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_open_id` (`account_id`, `open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号粉丝';

CREATE TABLE IF NOT EXISTS `wx_message_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL,
  `direction` varchar(10) NOT NULL COMMENT 'in/out',
  `open_id` varchar(64) DEFAULT NULL,
  `message_type` varchar(30) DEFAULT NULL,
  `event_type` varchar(50) DEFAULT NULL,
  `content` text,
  `raw_xml` longtext,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_time` (`account_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号消息日志';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '微信公众号功能开关', 'wechat.enabled', 'false', 'Y', 'admin', sysdate(), '是否启用微信公众号模块'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '微信公众号默认账号ID', 'wechat.defaultAccountId', '', 'Y', 'admin', sysdate(), '为空时需前端显式传 accountId'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.defaultAccountId');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '公众号回调密文模式', 'wechat.callback.encrypt', 'false', 'Y', 'admin', sysdate(), 'true=兼容/安全模式，false=明文模式'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.callback.encrypt');

INSERT IGNORE INTO sys_menu VALUES
(2050, '公众号', 0, 7, 'wechat-admin', NULL, '', 'WechatAdmin', 1, 0, 'M', '0', '0', '', 'wechat', 'admin', sysdate(), '', NULL, '微信公众号管理目录'),
(2051, '账号管理', 2050, 1, 'account', 'wechat/account/index', '', 'WechatAccount', 1, 0, 'C', '0', '0', 'wechat:account:list', 'user', 'admin', sysdate(), '', NULL, ''),
(2052, '推送记录', 2050, 2, 'publish', 'wechat/publish/index', '', 'WechatPublish', 1, 0, 'C', '0', '0', 'wechat:publish:list', 'message', 'admin', sysdate(), '', NULL, ''),
(2053, '素材管理', 2050, 3, 'material', 'wechat/material/index', '', 'WechatMaterial', 1, 0, 'C', '0', '0', 'wechat:material:list', 'documentation', 'admin', sysdate(), '', NULL, ''),
(2054, '菜单管理', 2050, 4, 'wx-menu', 'wechat/menu/index', '', 'WechatMenu', 1, 0, 'C', '0', '0', 'wechat:menu:list', 'tree', 'admin', sysdate(), '', NULL, ''),
(2055, '自动回复', 2050, 5, 'reply', 'wechat/reply/index', '', 'WechatReply', 1, 0, 'C', '0', '0', 'wechat:reply:list', 'edit', 'admin', sysdate(), '', NULL, ''),
(2056, '粉丝管理', 2050, 6, 'fans', 'wechat/fans/index', '', 'WechatFans', 1, 0, 'C', '0', '0', 'wechat:fans:list', 'peoples', 'admin', sysdate(), '', NULL, ''),
(2057, '消息日志', 2050, 7, 'message-log', 'wechat/message/index', '', 'WechatMessageLog', 1, 0, 'C', '0', '0', 'wechat:message:list', 'form', 'admin', sysdate(), '', NULL, ''),
(2300, '账号查询', 2051, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:query', '#', 'admin', sysdate(), '', NULL, ''),
(2301, '账号新增', 2051, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:add', '#', 'admin', sysdate(), '', NULL, ''),
(2302, '账号修改', 2051, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2303, '账号删除', 2051, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2304, '推送执行', 2052, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:publish:push', '#', 'admin', sysdate(), '', NULL, ''),
(2305, '素材删除', 2053, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:material:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2306, '菜单新增', 2054, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:add', '#', 'admin', sysdate(), '', NULL, ''),
(2307, '菜单修改', 2054, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2308, '菜单发布', 2054, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:publish', '#', 'admin', sysdate(), '', NULL, ''),
(2311, '菜单查询', 2054, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:query', '#', 'admin', sysdate(), '', NULL, ''),
(2312, '菜单删除', 2054, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2309, '回复新增', 2055, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:reply:add', '#', 'admin', sysdate(), '', NULL, ''),
(2310, '回复修改', 2055, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:reply:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2050 AND 2312;
