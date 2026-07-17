SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: 类目 / 品牌
-- 设计文档: docs/ecommerce-impl/phase1/

CREATE TABLE IF NOT EXISTS `mall_category` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '类目ID',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父类目ID，0为根',
  `name`        varchar(64)  NOT NULL COMMENT '类目名称',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `icon`        varchar(255) DEFAULT NULL COMMENT '图标URL',
  `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_mall_category_parent` (`parent_id`),
  KEY `idx_mall_category_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城类目';

CREATE TABLE IF NOT EXISTS `mall_brand` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '品牌ID',
  `name`        varchar(64)  NOT NULL COMMENT '品牌名称',
  `logo`        varchar(255) DEFAULT NULL COMMENT 'Logo URL',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_brand_name` (`name`),
  KEY `idx_mall_brand_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城品牌';
