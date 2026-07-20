SET NAMES utf8mb4;
USE nova_mall;

-- Phase B: 属性库 / 类目属性绑定 / 前台类目
-- 设计文档: docs/superpowers/specs/2026-07-20-mall-product-phase-b-design.md
-- 依赖: mall_category_brand_schema.sql, mall_product_schema.sql

CREATE TABLE IF NOT EXISTS `mall_attr` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '属性ID',
  `name`        varchar(64)  NOT NULL COMMENT '属性名称',
  `input_type`  varchar(16)  NOT NULL DEFAULT 'text' COMMENT 'text|select|multi',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `create_by`   varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_mall_attr_status` (`status`),
  KEY `idx_mall_attr_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城属性';

CREATE TABLE IF NOT EXISTS `mall_attr_option` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '选项ID',
  `attr_id`     bigint       NOT NULL COMMENT '属性ID',
  `value`       varchar(128) NOT NULL COMMENT '选项值',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  PRIMARY KEY (`id`),
  KEY `idx_mall_attr_option_attr` (`attr_id`),
  KEY `idx_mall_attr_option_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城属性选项';

CREATE TABLE IF NOT EXISTS `mall_category_attr` (
  `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint      NOT NULL COMMENT '后台类目ID（叶子）',
  `attr_id`     bigint      NOT NULL COMMENT '属性ID',
  `attr_type`   varchar(8)  NOT NULL COMMENT 'SALE|DESC',
  `required`    char(1)     NOT NULL DEFAULT '0' COMMENT '是否必填（0否 1是）',
  `sort`        int         NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_category_attr` (`category_id`, `attr_id`),
  KEY `idx_mall_category_attr_category` (`category_id`),
  KEY `idx_mall_category_attr_attr` (`attr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台类目属性绑定';

CREATE TABLE IF NOT EXISTS `mall_spu_attr_value` (
  `id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_id`  bigint       NOT NULL COMMENT 'SPU ID',
  `attr_id` bigint       NOT NULL COMMENT '属性ID',
  `value`   varchar(512) NOT NULL COMMENT '属性值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_spu_attr_value` (`spu_id`, `attr_id`),
  KEY `idx_mall_spu_attr_value_spu` (`spu_id`),
  KEY `idx_mall_spu_attr_value_attr` (`attr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SPU描述属性值';

CREATE TABLE IF NOT EXISTS `mall_front_category` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '前台类目ID',
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
  KEY `idx_mall_front_category_parent` (`parent_id`),
  KEY `idx_mall_front_category_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城前台类目';

CREATE TABLE IF NOT EXISTS `mall_front_category_rel` (
  `id`               bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `front_id`         bigint NOT NULL COMMENT '前台类目ID',
  `back_category_id` bigint NOT NULL COMMENT '后台类目ID，迁移含全部节点；运营映射建议叶子',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_front_category_rel` (`front_id`, `back_category_id`),
  KEY `idx_mall_front_category_rel_front` (`front_id`),
  KEY `idx_mall_front_category_rel_back` (`back_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前台类目与后台类目映射';
