SET NAMES utf8mb4;
USE nova_mall;

CREATE TABLE IF NOT EXISTS `wx_qrcode` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '公众号账号ID',
  `name` varchar(100) NOT NULL COMMENT '渠道名称',
  `qr_type` varchar(20) NOT NULL COMMENT 'temp/permanent',
  `scene_type` varchar(10) NOT NULL COMMENT 'int/str',
  `scene_id` int DEFAULT NULL COMMENT '整型场景值',
  `scene_str` varchar(64) DEFAULT NULL COMMENT '字符串场景值',
  `action_name` varchar(32) NOT NULL COMMENT '微信 action_name',
  `ticket` varchar(512) NOT NULL COMMENT '二维码 ticket',
  `url` varchar(512) DEFAULT NULL COMMENT '二维码解析地址',
  `expire_seconds` int DEFAULT NULL COMMENT '临时码有效期秒数',
  `expire_time` datetime DEFAULT NULL COMMENT '临时码过期时间',
  `scan_count` int NOT NULL DEFAULT 0 COMMENT '扫码次数',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account_update` (`account_id`, `update_time`),
  KEY `idx_account_scene_id` (`account_id`, `scene_id`),
  KEY `idx_account_scene_str` (`account_id`, `scene_str`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公众号带参数二维码';

INSERT IGNORE INTO sys_menu VALUES
(2058, '渠道二维码', 2050, 8, 'qrcode', 'wechat/qrcode/index', '', 'WechatQrcode', 1, 0, 'C', '0', '0', 'wechat:qrcode:list', 'qrcode', 'admin', sysdate(), '', NULL, ''),
(2315, '二维码新增', 2058, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:qrcode:add', '#', 'admin', sysdate(), '', NULL, ''),
(2316, '二维码删除', 2058, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:qrcode:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2058, 2315, 2316);
