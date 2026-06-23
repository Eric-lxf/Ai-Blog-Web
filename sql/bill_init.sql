-- ============================================================
-- blog_bill 表 + 菜单种子数据（menu_id 2400-2419）
-- ============================================================

CREATE TABLE IF NOT EXISTS blog_bill (
  id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bill_date      DATE          NOT NULL                              COMMENT '消费日期',
  merchant       VARCHAR(200)                                        COMMENT '商户名称',
  category       VARCHAR(50)   NOT NULL                              COMMENT '消费类目',
  amount         DECIMAL(12,2) NOT NULL                              COMMENT '消费金额',
  payment_method VARCHAR(50)                                         COMMENT '支付方式',
  note           VARCHAR(500)                                        COMMENT '备注',
  image_url      VARCHAR(500)                                        COMMENT '账单图片 URL',
  ai_confidence  TINYINT                                             COMMENT 'AI 识别置信度 0-100',
  source         TINYINT       NOT NULL DEFAULT 0                    COMMENT '0-手动录入 1-AI识别',
  user_id        BIGINT        NOT NULL                              COMMENT '所属用户',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT       NOT NULL DEFAULT 0,
  INDEX idx_user_date (user_id, bill_date)
) COMMENT='个人消费账单' DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_menu VALUES
(2400, '账单管理', 0,    6, 'bill',     NULL,                 '', '', 1, 0, 'M', '0', '0', '',                   'money', 'admin', sysdate(), '', NULL, '个人消费账单管理'),
(2401, '账单记录', 2400, 1, 'list',     'blog/bill/index',    '', '', 1, 0, 'C', '0', '0', 'blog:bill:list',      'list',  'admin', sysdate(), '', NULL, ''),
(2402, '消费分析', 2400, 2, 'analysis', 'blog/bill/analysis', '', '', 1, 0, 'C', '0', '0', 'blog:bill:analysis',  'chart', 'admin', sysdate(), '', NULL, ''),
(2410, '账单查询', 2401, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:query',     '#', 'admin', sysdate(), '', NULL, ''),
(2411, '账单新增', 2401, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:add',       '#', 'admin', sysdate(), '', NULL, ''),
(2412, '账单修改', 2401, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:edit',      '#', 'admin', sysdate(), '', NULL, ''),
(2413, '账单删除', 2401, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:remove',    '#', 'admin', sysdate(), '', NULL, ''),
(2414, 'AI识别',   2401, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:recognize', '#', 'admin', sysdate(), '', NULL, ''),
(2415, '分析查看', 2402, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:analysis',  '#', 'admin', sysdate(), '', NULL, '');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2400 AND menu_id < 2420;