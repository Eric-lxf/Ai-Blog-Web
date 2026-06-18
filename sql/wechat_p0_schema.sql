SET NAMES utf8mb4;
USE ai_blog;

CREATE TABLE IF NOT EXISTS `wx_media_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `name` varchar(100) NOT NULL COMMENT '素材名称',
  `media_type` varchar(20) NOT NULL COMMENT 'image/thumb/content',
  `media_id` varchar(128) DEFAULT NULL COMMENT '微信永久素材ID或正文图无ID',
  `url` varchar(1000) DEFAULT NULL COMMENT '微信CDN地址',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_type_time` (`account_id`, `media_type`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信公众号永久素材';

INSERT IGNORE INTO sys_menu VALUES
(2059, '草稿箱', 2050, 9, 'draft', 'wechat/draft/index', '', 'WechatDraft', 1, 0, 'C', '0', '0', 'wechat:draft:list', 'edit', 'admin', sysdate(), '', NULL, ''),
(2317, '素材上传', 2053, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:material:add', '#', 'admin', sysdate(), '', NULL, ''),
(2318, '素材查询', 2053, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:material:query', '#', 'admin', sysdate(), '', NULL, ''),
(2319, '草稿新增', 2059, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:draft:add', '#', 'admin', sysdate(), '', NULL, ''),
(2320, '草稿修改', 2059, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:draft:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2321, '草稿删除', 2059, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:draft:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2059, 2317, 2318, 2319, 2320, 2321);
