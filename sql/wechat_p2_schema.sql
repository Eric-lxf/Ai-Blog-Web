SET NAMES utf8mb4;
USE ai_blog;

ALTER TABLE `wx_fans`
  ADD COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '微信备注' AFTER `nickname`;

CREATE TABLE IF NOT EXISTS `wx_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `wechat_tag_id` int NOT NULL COMMENT '微信标签ID',
  `name` varchar(64) NOT NULL COMMENT '标签名称',
  `fan_count` int NOT NULL DEFAULT 0 COMMENT '粉丝数(同步自微信)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_wechat_tag` (`account_id`, `wechat_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信公众号用户标签';

CREATE TABLE IF NOT EXISTS `wx_fans_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fans_id` bigint NOT NULL COMMENT '本地粉丝ID',
  `tag_id` bigint NOT NULL COMMENT '本地标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fans_tag` (`fans_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='粉丝与标签关联';

CREATE TABLE IF NOT EXISTS `wx_mass_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `msg_type` varchar(20) NOT NULL COMMENT 'text/mpnews',
  `content` text COMMENT '文本内容',
  `media_id` varchar(128) DEFAULT NULL COMMENT '图文 media_id',
  `is_to_all` tinyint NOT NULL DEFAULT 0 COMMENT '是否全员发送',
  `wechat_tag_id` int DEFAULT NULL COMMENT '按标签发送时的微信 tag_id',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/sent/failed',
  `msg_id` bigint DEFAULT NULL COMMENT '微信 msg_id',
  `response_body` text COMMENT '微信响应 JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_time` (`account_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信群发记录';

INSERT IGNORE INTO sys_menu VALUES
(2061, '用户标签', 2050, 11, 'tag', 'wechat/tag/index', '', 'WechatTag', 1, 0, 'C', '0', '0', 'wechat:tag:list', 'peoples', 'admin', sysdate(), '', NULL, ''),
(2062, '客服消息', 2050, 12, 'kefu', 'wechat/kefu/index', '', 'WechatKefu', 1, 0, 'C', '0', '0', 'wechat:kefu:send', 'message', 'admin', sysdate(), '', NULL, ''),
(2063, '模板消息', 2050, 13, 'template', 'wechat/template/index', '', 'WechatTemplate', 1, 0, 'C', '0', '0', 'wechat:template:list', 'email', 'admin', sysdate(), '', NULL, ''),
(2064, '群发消息', 2050, 14, 'mass', 'wechat/mass/index', '', 'WechatMass', 1, 0, 'C', '0', '0', 'wechat:mass:list', 'guide', 'admin', sysdate(), '', NULL, ''),
(2326, '标签查询', 2061, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:tag:list', '#', 'admin', sysdate(), '', NULL, ''),
(2327, '标签新增', 2061, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:tag:add', '#', 'admin', sysdate(), '', NULL, ''),
(2328, '标签修改', 2061, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:tag:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2329, '标签删除', 2061, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:tag:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2330, '标签同步', 2061, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:tag:sync', '#', 'admin', sysdate(), '', NULL, ''),
(2331, '标签打标', 2061, 6, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:tag:mark', '#', 'admin', sysdate(), '', NULL, ''),
(2332, '客服发送', 2062, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:kefu:send', '#', 'admin', sysdate(), '', NULL, ''),
(2333, '模板查询', 2063, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:template:list', '#', 'admin', sysdate(), '', NULL, ''),
(2334, '模板发送', 2063, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:template:send', '#', 'admin', sysdate(), '', NULL, ''),
(2335, '群发查询', 2064, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:mass:list', '#', 'admin', sysdate(), '', NULL, ''),
(2336, '群发发送', 2064, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:mass:send', '#', 'admin', sysdate(), '', NULL, ''),
(2337, '群发预览', 2064, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:mass:preview', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2061, 2062, 2063, 2064, 2326, 2327, 2328, 2329, 2330, 2331, 2332, 2333, 2334, 2335, 2336, 2337);
