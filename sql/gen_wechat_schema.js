const fs = require('fs');
const path = require('path');

const h = (hex) => Buffer.from(hex, 'hex').toString('utf8');
const q = (s) => `'${String(s).replace(/'/g, "''")}'`;

const T = {
  accountName: h('e8b4a6e58fb7e5908de7a7b0'),
  wxAppId: h('e5beaee4bfa1e585ace4bc97e58fb76170704964'),
  wxAppSecret: h('e5beaee4bfa1e585ace4bc97e58fb7617070536563726574'),
  callbackToken: h('e59b9ee8b083746f6b656e'),
  aesKey: h('e6b688e681afe58aa0e8a7a3e5af866b6579'),
  enabled: h('e698afe590a6e590afe794a8'),
  wxAccount: h('e5beaee4bfa1e585ace4bc97e58fb7e8b4a6e58fb7'),
  draftMedia: h('e88d89e7a8bf2fe7b4a0e69d906d656469615f6964'),
  materialStatus: h('30e5be85e4b88ae4bca02031e88d89e7a8bfe68890e58a9f2032e5a4b1e8b4a5'),
  wxMaterial: h('e585ace4bc97e58fb7e59bbee69687e7b4a0e69d90'),
  articleId: h('e58d9ae5aea2e69687e7aba04944'),
  msgId: h('e58f91e5b883e4bbbbe58aa1e58fb7'),
  wxResponse: h('e5beaee4bfa1e8bf94e59b9ee58e9fe69687'),
  publishStatus: h('30e5be85e5a484e790862031e88d89e7a8bfe68890e58a9f2032e58f91e5b883e4b8ad2033e5b7b2e58f91e5b8832034e5a4b1e8b4a5'),
  publishRecord: h('e585ace4bc97e58fb7e68ea8e98081e8aeb0e5bd95'),
  menuJson: h('e88f9ce58d954a534f4e'),
  wxMenu: h('e585ace4bc97e58fb7e88f9ce58d95e9858de7bdae'),
  matchType: h('31e58c85e590abe58cb9e9858d2032e585a8e7ad89e58cb9e9858d'),
  autoReply: h('e585ace4bc97e58fb7e887aae58aa8e59b9ee5a48d'),
  fans: h('e585ace4bc97e58fb7e7b289e4b89d'),
  messageLog: h('e585ace4bc97e58fb7e6b688e681afe697a5e5bf97'),
  cfgEnabledName: h('e5beaee4bfa1e585ace4bc97e58fb7e58a9fe883bde5bc80e585b3'),
  cfgEnabledRemark: h('e698afe590a6e590afe794a8e5beaee4bfa1e585ace4bc97e58fb7e6a8a1e59d97'),
  cfgDefaultName: h('e5beaee4bfa1e585ace4bc97e58fb7e9bb98e8aea4e8b4a6e58fb74944'),
  cfgDefaultRemark: h('e4b8bae7a9bae697b6e99c80e5898de7abafe698bee5bc8fe4bca0206163636f756e744964'),
  cfgEncryptName: h('e585ace4bc97e58fb7e59b9ee8b083e5af86e69687e6a8a1e5bc8f'),
  cfgEncryptRemark: h('747275653de585bce5aeb92fe5ae89e585a8e6a8a1e5bc8fefbc8c66616c73653de6988ee69687e6a8a1e5bc8f'),
  menuRoot: h('e585ace4bc97e58fb7'),
  menuRootRemark: h('e5beaee4bfa1e585ace4bc97e58fb7e7aea1e79086e79baee5bd95'),
  menuAccount: h('e8b4a6e58fb7e7aea1e79086'),
  menuPublish: h('e68ea8e98081e8aeb0e5bd95'),
  menuMaterial: h('e7b4a0e69d90e7aea1e79086'),
  menuMenu: h('e88f9ce58d95e7aea1e79086'),
  menuReply: h('e887aae58aa8e59b9ee5a48d'),
  menuFans: h('e7b289e4b89de7aea1e79086'),
  menuMessage: h('e6b688e681afe697a5e5bf97'),
  btnAccountQuery: h('e8b4a6e58fb7e69fa5e8afa2'),
  btnAccountAdd: h('e8b4a6e58fb7e696b0e5a29e'),
  btnAccountEdit: h('e8b4a6e58fb7e4bfaee694b9'),
  btnAccountRemove: h('e8b4a6e58fb7e588a0e999a4'),
  btnPublishPush: h('e68ea8e98081e689a7e8a18c'),
  btnMaterialRemove: h('e7b4a0e69d90e588a0e999a4'),
  btnMenuAdd: h('e88f9ce58d95e696b0e5a29e'),
  btnMenuEdit: h('e88f9ce58d95e4bfaee694b9'),
  btnMenuPublish: h('e88f9ce58d95e58f91e5b883'),
  btnReplyAdd: h('e59b9ee5a48de696b0e5a29e'),
  btnReplyEdit: h('e59b9ee5a48de4bfaee694b9'),
};

const sql = `SET NAMES utf8mb4;
USE ai_blog;

CREATE TABLE IF NOT EXISTS \`wx_account\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`name\` varchar(100) NOT NULL COMMENT ${q(T.accountName)},
  \`app_id\` varchar(64) NOT NULL COMMENT ${q(T.wxAppId)},
  \`app_secret\` varchar(128) NOT NULL COMMENT ${q(T.wxAppSecret)},
  \`token\` varchar(64) NOT NULL COMMENT ${q(T.callbackToken)},
  \`aes_key\` varchar(64) DEFAULT NULL COMMENT ${q(T.aesKey)},
  \`enabled\` tinyint NOT NULL DEFAULT 1 COMMENT ${q(T.enabled)},
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_app_id\` (\`app_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.wxAccount)};

CREATE TABLE IF NOT EXISTS \`wx_material\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`account_id\` bigint NOT NULL,
  \`title\` varchar(255) NOT NULL,
  \`thumb_media_id\` varchar(128) DEFAULT NULL,
  \`author\` varchar(100) DEFAULT NULL,
  \`digest\` varchar(500) DEFAULT NULL,
  \`content\` longtext NOT NULL,
  \`content_source_url\` varchar(500) DEFAULT NULL,
  \`media_id\` varchar(128) DEFAULT NULL COMMENT ${q(T.draftMedia)},
  \`url\` varchar(500) DEFAULT NULL,
  \`status\` tinyint NOT NULL DEFAULT 0 COMMENT ${q(T.materialStatus)},
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  KEY \`idx_account_update\` (\`account_id\`, \`update_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.wxMaterial)};

CREATE TABLE IF NOT EXISTS \`wx_publish_record\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`account_id\` bigint NOT NULL,
  \`article_id\` bigint NOT NULL COMMENT ${q(T.articleId)},
  \`material_id\` bigint DEFAULT NULL,
  \`publish_mode\` varchar(50) NOT NULL COMMENT 'draft / draft_and_publish',
  \`msg_id\` varchar(128) DEFAULT NULL COMMENT ${q(T.msgId)},
  \`response_body\` longtext DEFAULT NULL COMMENT ${q(T.wxResponse)},
  \`status\` tinyint NOT NULL DEFAULT 0 COMMENT ${q(T.publishStatus)},
  \`error_message\` varchar(500) DEFAULT NULL,
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  KEY \`idx_account_status_time\` (\`account_id\`, \`status\`, \`update_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.publishRecord)};

CREATE TABLE IF NOT EXISTS \`wx_menu\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`account_id\` bigint NOT NULL,
  \`menu_json\` longtext NOT NULL COMMENT ${q(T.menuJson)},
  \`is_published\` tinyint NOT NULL DEFAULT 0,
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  KEY \`idx_account_update\` (\`account_id\`, \`update_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.wxMenu)};

CREATE TABLE IF NOT EXISTS \`wx_auto_reply\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`account_id\` bigint NOT NULL,
  \`reply_type\` varchar(20) NOT NULL COMMENT 'keyword/default/subscribe',
  \`keyword\` varchar(100) DEFAULT NULL,
  \`content\` varchar(1000) NOT NULL,
  \`enabled\` tinyint NOT NULL DEFAULT 1,
  \`match_type\` tinyint NOT NULL DEFAULT 1 COMMENT ${q(T.matchType)},
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  KEY \`idx_account_type\` (\`account_id\`, \`reply_type\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.autoReply)};

CREATE TABLE IF NOT EXISTS \`wx_fans\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`account_id\` bigint NOT NULL,
  \`open_id\` varchar(64) NOT NULL,
  \`union_id\` varchar(64) DEFAULT NULL,
  \`nickname\` varchar(255) DEFAULT NULL,
  \`subscribe_status\` tinyint NOT NULL DEFAULT 1,
  \`subscribe_time\` datetime DEFAULT NULL,
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`update_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  UNIQUE KEY \`uk_account_open_id\` (\`account_id\`, \`open_id\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.fans)};

CREATE TABLE IF NOT EXISTS \`wx_message_log\` (
  \`id\` bigint NOT NULL AUTO_INCREMENT,
  \`account_id\` bigint NOT NULL,
  \`direction\` varchar(10) NOT NULL COMMENT 'in/out',
  \`open_id\` varchar(64) DEFAULT NULL,
  \`message_type\` varchar(30) DEFAULT NULL,
  \`event_type\` varchar(50) DEFAULT NULL,
  \`content\` text,
  \`raw_xml\` longtext,
  \`create_time\` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (\`id\`),
  KEY \`idx_account_time\` (\`account_id\`, \`create_time\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=${q(T.messageLog)};

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT ${q(T.cfgEnabledName)}, 'wechat.enabled', 'false', 'Y', 'admin', sysdate(), ${q(T.cfgEnabledRemark)}
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT ${q(T.cfgDefaultName)}, 'wechat.defaultAccountId', '', 'Y', 'admin', sysdate(), ${q(T.cfgDefaultRemark)}
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.defaultAccountId');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT ${q(T.cfgEncryptName)}, 'wechat.callback.encrypt', 'false', 'Y', 'admin', sysdate(), ${q(T.cfgEncryptRemark)}
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'wechat.callback.encrypt');

INSERT IGNORE INTO sys_menu VALUES
(2050, ${q(T.menuRoot)}, 0, 7, 'wechat-admin', NULL, '', '', 1, 0, 'M', '0', '0', '', 'wechat', 'admin', sysdate(), '', NULL, ${q(T.menuRootRemark)}),
(2051, ${q(T.menuAccount)}, 2050, 1, 'account', 'wechat/account/index', '', '', 1, 0, 'C', '0', '0', 'wechat:account:list', 'user', 'admin', sysdate(), '', NULL, ''),
(2052, ${q(T.menuPublish)}, 2050, 2, 'publish', 'wechat/publish/index', '', '', 1, 0, 'C', '0', '0', 'wechat:publish:list', 'message', 'admin', sysdate(), '', NULL, ''),
(2053, ${q(T.menuMaterial)}, 2050, 3, 'material', 'wechat/material/index', '', '', 1, 0, 'C', '0', '0', 'wechat:material:list', 'documentation', 'admin', sysdate(), '', NULL, ''),
(2054, ${q(T.menuMenu)}, 2050, 4, 'menu', 'wechat/menu/index', '', '', 1, 0, 'C', '0', '0', 'wechat:menu:list', 'tree', 'admin', sysdate(), '', NULL, ''),
(2055, ${q(T.menuReply)}, 2050, 5, 'reply', 'wechat/reply/index', '', '', 1, 0, 'C', '0', '0', 'wechat:reply:list', 'edit', 'admin', sysdate(), '', NULL, ''),
(2056, ${q(T.menuFans)}, 2050, 6, 'fans', 'wechat/fans/index', '', '', 1, 0, 'C', '0', '0', 'wechat:fans:list', 'peoples', 'admin', sysdate(), '', NULL, ''),
(2057, ${q(T.menuMessage)}, 2050, 7, 'message-log', 'wechat/message/index', '', '', 1, 0, 'C', '0', '0', 'wechat:message:list', 'form', 'admin', sysdate(), '', NULL, ''),
(2300, ${q(T.btnAccountQuery)}, 2051, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:query', '#', 'admin', sysdate(), '', NULL, ''),
(2301, ${q(T.btnAccountAdd)}, 2051, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:add', '#', 'admin', sysdate(), '', NULL, ''),
(2302, ${q(T.btnAccountEdit)}, 2051, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2303, ${q(T.btnAccountRemove)}, 2051, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:account:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2304, ${q(T.btnPublishPush)}, 2052, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:publish:push', '#', 'admin', sysdate(), '', NULL, ''),
(2305, ${q(T.btnMaterialRemove)}, 2053, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:material:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2306, ${q(T.btnMenuAdd)}, 2054, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:add', '#', 'admin', sysdate(), '', NULL, ''),
(2307, ${q(T.btnMenuEdit)}, 2054, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2308, ${q(T.btnMenuPublish)}, 2054, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:menu:publish', '#', 'admin', sysdate(), '', NULL, ''),
(2309, ${q(T.btnReplyAdd)}, 2055, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:reply:add', '#', 'admin', sysdate(), '', NULL, ''),
(2310, ${q(T.btnReplyEdit)}, 2055, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'wechat:reply:edit', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2050 AND 2310;
`;

const out = path.join(__dirname, 'wechat_schema.sql');
fs.writeFileSync(out, sql, 'utf8');
console.log('written', out, 'sample', T.menuRoot);
