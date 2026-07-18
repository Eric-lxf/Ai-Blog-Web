SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: 支付单（与业务订单解耦）
-- 依赖: 04_mall_cart_order_schema.sql

CREATE TABLE IF NOT EXISTS `mall_payment_order` (
  `id`                bigint         NOT NULL AUTO_INCREMENT COMMENT '支付单ID',
  `pay_no`            varchar(32)    NOT NULL COMMENT '支付单号',
  `order_id`          bigint         NOT NULL COMMENT '业务订单ID',
  `order_no`          varchar(32)    NOT NULL COMMENT '业务订单号（冗余）',
  `user_id`           bigint         NOT NULL COMMENT '用户ID',
  `channel`           varchar(16)    NOT NULL COMMENT 'WECHAT/ALIPAY/MOCK',
  `amount`            decimal(12,2)  NOT NULL COMMENT '支付金额（元）',
  `status`            varchar(16)    NOT NULL DEFAULT 'INIT' COMMENT 'INIT/PAYING/SUCCESS/FAILED/CLOSED',
  `channel_trade_no`  varchar(64)    DEFAULT NULL COMMENT '渠道交易号',
  `notify_raw`        mediumtext     COMMENT '回调原始报文',
  `paid_time`         datetime       DEFAULT NULL COMMENT '渠道确认成功时间',
  `expire_time`       datetime       DEFAULT NULL COMMENT '支付单过期时间',
  `create_time`       datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`            varchar(500)   DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_pay_no` (`pay_no`),
  KEY `idx_mall_pay_order_id` (`order_id`),
  KEY `idx_mall_pay_status` (`status`),
  KEY `idx_mall_pay_channel_trade` (`channel`, `channel_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城支付单';
