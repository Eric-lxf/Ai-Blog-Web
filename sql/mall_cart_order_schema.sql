SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: 购物车 / 订单 / 订单项 / 订单日志
-- 依赖: 02_mall_product_schema.sql, 03_mall_address_schema.sql（地址仅业务依赖，无 FK）

CREATE TABLE IF NOT EXISTS `mall_cart` (
  `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `user_id`     bigint   NOT NULL COMMENT '用户ID（sys_user.user_id）',
  `sku_id`      bigint   NOT NULL COMMENT 'SKU ID',
  `quantity`    int      NOT NULL DEFAULT 1 COMMENT '数量',
  `checked`     char(1)  NOT NULL DEFAULT '1' COMMENT '是否勾选（0否 1是）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_cart_user_sku` (`user_id`, `sku_id`),
  KEY `idx_mall_cart_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城购物车';

CREATE TABLE IF NOT EXISTS `mall_order` (
  `id`                    bigint         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`              varchar(32)    NOT NULL COMMENT '订单号',
  `user_id`               bigint         NOT NULL COMMENT '用户ID',
  `status`                varchar(32)    NOT NULL COMMENT 'PENDING_PAY/PAID/SHIPPED/COMPLETED/CANCELLED',
  `pay_amount`            decimal(12,2)  NOT NULL DEFAULT 0.00 COMMENT '应付金额（元）',
  `goods_amount`          decimal(12,2)  NOT NULL DEFAULT 0.00 COMMENT '商品总额（元）',
  `freight_amount`        decimal(12,2)  NOT NULL DEFAULT 0.00 COMMENT '运费（P1可为0）',
  `address_snapshot`      json           DEFAULT NULL COMMENT '收货地址快照JSON',
  `pay_time`              datetime       DEFAULT NULL COMMENT '支付成功时间',
  `ship_time`             datetime       DEFAULT NULL COMMENT '发货时间',
  `complete_time`         datetime       DEFAULT NULL COMMENT '完成时间',
  `cancel_time`           datetime       DEFAULT NULL COMMENT '取消时间',
  `expire_time`           datetime       DEFAULT NULL COMMENT '待支付过期时间',
  `cancel_reason`         varchar(255)   DEFAULT NULL COMMENT '取消原因',
  `create_by`             varchar(64)    DEFAULT '' COMMENT '创建者',
  `create_time`           datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`             varchar(64)    DEFAULT '' COMMENT '更新者',
  `update_time`           datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`                varchar(500)   DEFAULT NULL COMMENT '备注',
  `del_flag`              char(1)        NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_order_no` (`order_no`),
  KEY `idx_mall_order_user` (`user_id`),
  KEY `idx_mall_order_status` (`status`),
  KEY `idx_mall_order_expire` (`status`, `expire_time`),
  KEY `idx_mall_order_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单';

CREATE TABLE IF NOT EXISTS `mall_order_item` (
  `id`          bigint         NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id`    bigint         NOT NULL COMMENT '订单ID',
  `spu_id`      bigint         DEFAULT NULL COMMENT 'SPU ID（追溯）',
  `sku_id`      bigint         NOT NULL COMMENT 'SKU ID（追溯）',
  `spu_name`    varchar(128)   NOT NULL COMMENT '商品名称快照',
  `sku_specs`   varchar(512)   DEFAULT NULL COMMENT '规格快照',
  `sku_code`    varchar(64)    DEFAULT NULL COMMENT 'SKU编码快照',
  `image`       varchar(512)   DEFAULT NULL COMMENT '图片快照',
  `price`       decimal(12,2)  NOT NULL COMMENT '成交单价快照',
  `quantity`    int            NOT NULL COMMENT '数量',
  `create_time` datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mall_order_item_order` (`order_id`),
  KEY `idx_mall_order_item_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单项（快照）';

CREATE TABLE IF NOT EXISTS `mall_order_log` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `order_id`    bigint       NOT NULL COMMENT '订单ID',
  `from_status` varchar(32)  DEFAULT NULL COMMENT '原状态',
  `to_status`   varchar(32)  NOT NULL COMMENT '目标状态',
  `remark`      varchar(500) DEFAULT NULL COMMENT '说明',
  `create_by`   varchar(64)  DEFAULT '' COMMENT '操作人',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mall_order_log_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城订单状态流水';
