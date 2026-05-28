SET NAMES utf8mb4;
USE ai_blog;

-- 评论主表
CREATE TABLE IF NOT EXISTS `blog_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `root_id` bigint DEFAULT NULL COMMENT '根评论ID',
  `user_id` bigint DEFAULT NULL COMMENT '登录用户ID',
  `guest_name` varchar(64) DEFAULT NULL COMMENT '匿名昵称',
  `guest_email` varchar(128) DEFAULT NULL COMMENT '匿名邮箱',
  `content` varchar(2000) NOT NULL COMMENT '评论内容',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝 3已隐藏 4垃圾',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` int NOT NULL DEFAULT 0 COMMENT '回复数',
  `sort_score` double NOT NULL DEFAULT 0 COMMENT '热度分',
  `ip` varchar(64) DEFAULT NULL COMMENT 'IP',
  `user_agent` varchar(512) DEFAULT NULL COMMENT 'UA',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `ai_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未检测 1检测中 2通过 3疑似 4高风险',
  `ai_score` int DEFAULT NULL COMMENT 'AI风险分0-100',
  `ai_label` varchar(500) DEFAULT NULL COMMENT 'AI标签JSON',
  `ai_checked_time` datetime DEFAULT NULL COMMENT 'AI检测时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_article_status_time` (`article_id`, `status`, `create_time`),
  KEY `idx_root_id` (`root_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_ai_status` (`ai_status`, `ai_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客评论';

CREATE TABLE IF NOT EXISTS `blog_comment_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `guest_key` varchar(64) DEFAULT NULL COMMENT '匿名点赞标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
  UNIQUE KEY `uk_comment_guest` (`comment_id`, `guest_key`),
  KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞';

CREATE TABLE IF NOT EXISTS `blog_comment_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `reporter_user_id` bigint DEFAULT NULL,
  `reporter_guest_key` varchar(64) DEFAULT NULL,
  `reason` varchar(500) NOT NULL COMMENT '举报原因',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
  `handle_remark` varchar(500) DEFAULT NULL,
  `handle_by` varchar(64) DEFAULT NULL,
  `handle_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_id` (`comment_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论举报';

CREATE TABLE IF NOT EXISTS `blog_sensitive_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL COMMENT '敏感词',
  `match_mode` varchar(20) NOT NULL DEFAULT 'contains' COMMENT 'exact/contains',
  `action` varchar(20) NOT NULL DEFAULT 'block' COMMENT 'block/replace/review',
  `replace_text` varchar(100) DEFAULT NULL COMMENT '替换文本',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论敏感词';

-- 评论系统参数
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '评论先审后发', 'blog.comment.requireAudit', 'true', 'Y', 'admin', sysdate(), 'true=待审核后可见'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.requireAudit');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '允许匿名评论', 'blog.comment.anonymous.enabled', 'true', 'Y', 'admin', sysdate(), '是否允许匿名评论'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.anonymous.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '评论最大长度', 'blog.comment.maxLength', '2000', 'Y', 'admin', sysdate(), '单条评论最大字符数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.maxLength');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '评论频率限制', 'blog.comment.rateLimitPerMinute', '5', 'Y', 'admin', sysdate(), '每IP/用户每分钟最多评论数'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.rateLimitPerMinute');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '评论AI审核', 'blog.comment.ai.enabled', 'true', 'Y', 'admin', sysdate(), '是否启用AI评论审核'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.ai.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'AI自动拒绝阈值', 'blog.comment.ai.autoRejectScore', '80', 'Y', 'admin', sysdate(), 'AI风险分>=该值自动标记垃圾/拒绝'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.ai.autoRejectScore');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'AI自动通过阈值', 'blog.comment.ai.autoPassScore', '20', 'Y', 'admin', sysdate(), '关闭先审后发时AI低分自动通过'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.comment.ai.autoPassScore');

INSERT INTO ai_prompt_template (template_name, scene_type, system_prompt, model_name, temperature, is_active)
SELECT '评论审核', 'COMMENT_MODERATE',
  '你是博客评论内容审核助手。根据评论正文判断是否存在广告、辱骂、色情、政治敏感、垃圾灌水、钓鱼链接等风险。严格输出 JSON：{"riskScore":0-100,"labels":[],"suggestion":"pass|review|reject","reason":""}。不要输出其它文字。',
  'deepseek-chat', 0.30, 1
WHERE NOT EXISTS (SELECT 1 FROM ai_prompt_template WHERE scene_type = 'COMMENT_MODERATE');

-- 评论管理菜单 menu_id 2010+（挂在「博客」2030 下，path 勿使用 comment/xxx 嵌套）
INSERT IGNORE INTO sys_menu VALUES
(2030, '博客', 0, 6, 'blog-ops', NULL, '', '', 1, 0, 'M', '0', '0', '', 'list', 'admin', sysdate(), '', NULL, '博客运营'),
(2031, '博客列表', 2030, 1, 'list', 'blog/list/index', '', '', 1, 0, 'C', '0', '0', 'blog:article:list', 'documentation', 'admin', sysdate(), '', NULL, ''),
(2010, '评论管理', 2030, 2, 'comment-manage', 'blog/comment/index', '', '', 1, 0, 'C', '0', '0', 'blog:comment:list', 'message', 'admin', sysdate(), '', NULL, ''),
(2011, '举报处理', 2030, 3, 'comment-report', 'blog/comment/report', '', '', 1, 0, 'C', '0', '0', 'blog:comment:report:list', 'bell', 'admin', sysdate(), '', NULL, ''),
(2012, '敏感词管理', 2030, 4, 'comment-sensitive', 'blog/comment/sensitive', '', '', 1, 0, 'C', '0', '0', 'blog:sensitive:list', 'lock', 'admin', sysdate(), '', NULL, ''),
(2200, '评论查询', 2010, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:comment:list', '#', 'admin', sysdate(), '', NULL, ''),
(2201, '评论审核', 2010, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:comment:audit', '#', 'admin', sysdate(), '', NULL, ''),
(2202, '评论删除', 2010, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:comment:remove', '#', 'admin', sysdate(), '', NULL, ''),
(2203, '举报查询', 2011, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:comment:report:list', '#', 'admin', sysdate(), '', NULL, ''),
(2204, '举报处理', 2011, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:comment:report:handle', '#', 'admin', sysdate(), '', NULL, ''),
(2210, '敏感词查询', 2012, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:sensitive:list', '#', 'admin', sysdate(), '', NULL, ''),
(2211, '敏感词新增', 2012, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:sensitive:add', '#', 'admin', sysdate(), '', NULL, ''),
(2212, '敏感词修改', 2012, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:sensitive:edit', '#', 'admin', sysdate(), '', NULL, ''),
(2213, '敏感词删除', 2012, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:sensitive:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2010 AND 2213 OR menu_id IN (2030, 2031);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(3, 2030), (3, 2031), (3, 2010), (3, 2011), (3, 2012),
(3, 2200), (3, 2201), (3, 2202), (3, 2203), (3, 2204),
(3, 2210), (3, 2211), (3, 2212), (3, 2213);
