-- ============================================================
-- blog_file 表 + 菜单种子数据（menu_id 2420-2429）
-- ============================================================

CREATE TABLE IF NOT EXISTS blog_file (
  id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  file_name   VARCHAR(500)  NOT NULL                              COMMENT '原始文件名',
  file_key    VARCHAR(1000)                                       COMMENT 'OSS Key 或本地相对路径',
  file_url    VARCHAR(1000) NOT NULL                              COMMENT '可访问 URL',
  file_type   VARCHAR(100)                                        COMMENT 'MIME 类型',
  file_size   BIGINT                                              COMMENT '文件大小（字节）',
  user_id     BIGINT        NOT NULL                              COMMENT '所属用户',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted  TINYINT       NOT NULL DEFAULT 0,
  INDEX idx_user (user_id)
) COMMENT='文件管理' DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_menu VALUES
(2420, '文件管理', 0,    7, 'file', NULL,              '', '', 1, 0, 'M', '0', '0', '',                   'upload', 'admin', sysdate(), '', NULL, '文件上传与管理'),
(2421, '文件列表', 2420, 1, 'list', 'blog/file/index', '', '', 1, 0, 'C', '0', '0', 'blog:file:list',      'list',   'admin', sysdate(), '', NULL, ''),
(2422, '文件上传', 2421, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:file:upload', '#', 'admin', sysdate(), '', NULL, ''),
(2423, '文件删除', 2421, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:file:remove', '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2420 AND menu_id < 2430;