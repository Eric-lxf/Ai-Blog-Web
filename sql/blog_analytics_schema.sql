SET NAMES utf8mb4;
USE nova_mall;

-- 访问日志（PV/UV、来源、地域）
CREATE TABLE IF NOT EXISTS blog_visit_log (
  id bigint NOT NULL AUTO_INCREMENT,
  page_type varchar(20) NOT NULL COMMENT 'HOME/LIST/ARTICLE',
  article_id bigint DEFAULT NULL COMMENT '文章ID，首页/列表为空',
  visitor_key varchar(64) NOT NULL COMMENT '访客标识（前端UUID或IP+UA摘要）',
  user_id bigint DEFAULT NULL COMMENT '登录用户ID',
  ip varchar(64) DEFAULT NULL,
  user_agent varchar(512) DEFAULT NULL,
  referer varchar(500) DEFAULT NULL,
  referer_host varchar(128) DEFAULT NULL COMMENT '来源站点',
  region varchar(128) DEFAULT NULL COMMENT '地域',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_create_time (create_time),
  KEY idx_page_article_time (page_type, article_id, create_time),
  KEY idx_visitor_time (visitor_key, create_time),
  KEY idx_referer_host_time (referer_host, create_time),
  KEY idx_region_time (region, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客访问日志';

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '博客访问统计', 'blog.analytics.enabled', 'true', 'Y', 'admin', sysdate(), '关闭后不再写入访问日志'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'blog.analytics.enabled');
