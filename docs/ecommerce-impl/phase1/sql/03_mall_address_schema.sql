SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: 收货地址（挂 sys_user.user_id，方案 A）

CREATE TABLE IF NOT EXISTS `mall_address` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id`     bigint       NOT NULL COMMENT '用户ID（sys_user.user_id）',
  `receiver`    varchar(64)  NOT NULL COMMENT '收货人',
  `mobile`      varchar(20)  NOT NULL COMMENT '手机号',
  `province`    varchar(64)  NOT NULL COMMENT '省',
  `city`        varchar(64)  NOT NULL COMMENT '市',
  `district`    varchar(64)  NOT NULL COMMENT '区/县',
  `detail`      varchar(255) NOT NULL COMMENT '详细地址',
  `is_default`  char(1)      NOT NULL DEFAULT '0' COMMENT '是否默认（0否 1是）',
  `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_mall_address_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城收货地址';
