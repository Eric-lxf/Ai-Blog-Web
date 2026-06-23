SET NAMES utf8mb4;
USE ai_blog;

-- Bill table
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

-- Menu seed data (menu_id 2200-2220)
INSERT IGNORE INTO sys_menu VALUES
(2200, '账单管理',  0,    6, 'bill',     NULL,                 '', '', 1, 0, 'M', '0', '0', '',                   'money', 'admin', sysdate(), '', NULL, '个人消费账单管理'),
(2201, '账单记录',  2200, 1, 'list',     'blog/bill/index',    '', '', 1, 0, 'C', '0', '0', 'blog:bill:list',      'list',  'admin', sysdate(), '', NULL, ''),
(2202, '消费分析',  2200, 2, 'analysis', 'blog/bill/analysis', '', '', 1, 0, 'C', '0', '0', 'blog:bill:analysis',  'chart', 'admin', sysdate(), '', NULL, ''),
(2210, '账单查询',  2201, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:query',     '#', 'admin', sysdate(), '', NULL, ''),
(2211, '账单新增',  2201, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:add',       '#', 'admin', sysdate(), '', NULL, ''),
(2212, '账单修改',  2201, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:edit',      '#', 'admin', sysdate(), '', NULL, ''),
(2213, '账单删除',  2201, 4, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:remove',    '#', 'admin', sysdate(), '', NULL, ''),
(2214, 'AI识别',  2201, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:recognize', '#', 'admin', sysdate(), '', NULL, ''),
(2220, '分析查看',  2202, 1, '#', '', '', '', 1, 0, 'F', '0', '0', 'blog:bill:analysis',  '#', 'admin', sysdate(), '', NULL, '');

-- Grant to super admin (role_id=1)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2200 AND menu_id < 2300;