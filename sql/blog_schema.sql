SET NAMES utf8mb4;
USE ai_blog;

CREATE TABLE IF NOT EXISTS `blog_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT '文章正文(Markdown)',
  `html_content` longtext COMMENT '解析后的HTML',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图URL',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-AI生成中',
  `is_ai_generated` tinyint DEFAULT 0 COMMENT '是否由AI主导生成',
  `view_count` int DEFAULT 0 COMMENT '浏览量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客文章主表';

CREATE TABLE IF NOT EXISTS `blog_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

CREATE TABLE IF NOT EXISTS `blog_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签表';

CREATE TABLE IF NOT EXISTS `blog_article_tag` (
  `article_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `scene_type` varchar(50) NOT NULL COMMENT '场景标识',
  `system_prompt` text NOT NULL COMMENT 'System Prompt',
  `model_name` varchar(50) DEFAULT 'deepseek-chat',
  `temperature` decimal(3,2) DEFAULT 0.70,
  `is_active` tinyint DEFAULT 1,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词模板表';

CREATE TABLE IF NOT EXISTS `ai_task_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_type` varchar(50) NOT NULL COMMENT '任务类型',
  `target_article_id` bigint DEFAULT NULL,
  `prompt_payload` text COMMENT '请求参数JSON',
  `intermediate_data` json DEFAULT NULL COMMENT '中间数据(如大纲)',
  `result_content` longtext COMMENT '生成结果',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-排队,1-生成中,2-成功,3-失败',
  `error_message` text,
  `tokens_used` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `finish_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务记录表';

INSERT INTO `blog_category` (`name`, `sort_order`) VALUES ('未分类', 0), ('技术笔记', 1), ('AI探索', 2)
ON DUPLICATE KEY UPDATE `sort_order` = VALUES(`sort_order`);

INSERT INTO `ai_prompt_template` (`template_name`, `scene_type`, `system_prompt`, `model_name`, `temperature`, `is_active`)
SELECT * FROM (
  SELECT '博客写作助手' AS template_name, 'CHAT' AS scene_type,
    '你是一位资深技术博客写作助手，擅长 Java、架构设计、Markdown 与 Mermaid 图表。回答简洁专业，可直接给出可粘贴进博客的正文片段。若用户要求画图，请输出 ```mermaid 代码块。' AS system_prompt,
    'deepseek-chat' AS model_name, 0.70 AS temperature, 1 AS is_active
  UNION ALL
  SELECT '技术文档润色', 'REWRITE',
    '你是技术文档编辑，在保留原意和结构的前提下润色文字，修正错别字与语法，使表达更专业流畅。只输出润色后的正文，不要解释。',
    'deepseek-chat', 0.50, 1
  UNION ALL
  SELECT '生成文章摘要', 'SUMMARY',
    '根据用户提供的文章标题和正文，生成一段 80-150 字的 SEO 友好摘要。只输出摘要文本。',
    'deepseek-chat', 0.60, 1
  UNION ALL
  SELECT '内容扩写', 'EXPAND',
    '将用户给出的段落扩写为更完整的技术博客内容，补充必要背景与细节。只输出扩写后的正文，不要解释。',
    'deepseek-chat', 0.70, 1
  UNION ALL
  SELECT '内容精简', 'SHORTEN',
    '在不丢失核心信息的前提下精简用户给出的段落，使表达更紧凑。只输出精简后的正文，不要解释。',
    'deepseek-chat', 0.50, 1
  UNION ALL
  SELECT '智能续写', 'CONTINUE',
    '根据用户给出的上文，用相同风格续写 1-3 段技术博客内容。只输出续写正文，不要重复上文，不要解释。',
    'deepseek-chat', 0.75, 1
  UNION ALL
  SELECT 'Mermaid 图表', 'MERMAID_GEN',
    '根据用户描述生成 Mermaid 图表。只输出一个 ```mermaid 代码块，不要其他说明。',
    'deepseek-chat', 0.60, 1
  UNION ALL
  SELECT '代码生成', 'CODE_GEN',
    '根据用户需求生成代码示例。只输出一个带语言标注的 Markdown 代码块，不要其他说明。',
    'deepseek-chat', 0.50, 1
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `ai_prompt_template` WHERE `scene_type` = 'CHAT' LIMIT 1);

INSERT INTO `ai_prompt_template` (`template_name`, `scene_type`, `system_prompt`, `model_name`, `temperature`, `is_active`)
SELECT '标题生成', 'TITLE_GEN', '你是技术博客标题策划，擅长输出吸引技术读者的中文标题。严格按用户要求格式返回 JSON。', 'deepseek-chat', 0.80, 1
WHERE NOT EXISTS (SELECT 1 FROM `ai_prompt_template` WHERE `scene_type` = 'TITLE_GEN');

INSERT INTO `ai_prompt_template` (`template_name`, `scene_type`, `system_prompt`, `model_name`, `temperature`, `is_active`)
SELECT '大纲生成', 'OUTLINE_GEN', '你是技术博客结构编辑，擅长设计层次清晰的大纲。严格按用户要求返回 JSON 数组。', 'deepseek-chat', 0.60, 1
WHERE NOT EXISTS (SELECT 1 FROM `ai_prompt_template` WHERE `scene_type` = 'OUTLINE_GEN');

INSERT INTO `ai_prompt_template` (`template_name`, `scene_type`, `system_prompt`, `model_name`, `temperature`, `is_active`)
SELECT '全文生成', 'FULL_ARTICLE', '你是资深技术作者，根据大纲撰写 Markdown 博客，含代码与 mermaid 图。只输出正文 Markdown。', 'deepseek-chat', 0.70, 1
WHERE NOT EXISTS (SELECT 1 FROM `ai_prompt_template` WHERE `scene_type` = 'FULL_ARTICLE');
