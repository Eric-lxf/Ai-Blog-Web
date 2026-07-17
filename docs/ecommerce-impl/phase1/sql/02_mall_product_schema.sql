SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: SPU / SKU / 图片
-- 依赖: 01_mall_category_brand_schema.sql

CREATE TABLE IF NOT EXISTS `mall_spu` (
  `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT 'SPU ID',
  `category_id`  bigint        NOT NULL COMMENT '类目ID',
  `brand_id`     bigint        DEFAULT NULL COMMENT '品牌ID',
  `name`         varchar(128)  NOT NULL COMMENT '商品名称',
  `subtitle`     varchar(255)  DEFAULT NULL COMMENT '副标题',
  `main_image`   varchar(512)  DEFAULT NULL COMMENT '主图URL',
  `detail_html`  mediumtext    COMMENT '详情HTML',
  `status`       varchar(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿 ON上架 OFF下架',
  `sort`         int           NOT NULL DEFAULT 0 COMMENT '排序',
  `create_by`    varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`  datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`    varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`  datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`       varchar(500)  DEFAULT NULL COMMENT '备注',
  `del_flag`     char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_mall_spu_category` (`category_id`),
  KEY `idx_mall_spu_brand` (`brand_id`),
  KEY `idx_mall_spu_status` (`status`),
  KEY `idx_mall_spu_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城SPU';

CREATE TABLE IF NOT EXISTS `mall_sku` (
  `id`          bigint         NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
  `spu_id`      bigint         NOT NULL COMMENT 'SPU ID',
  `sku_code`    varchar(64)    NOT NULL COMMENT 'SKU编码',
  `specs_json`  varchar(512)   DEFAULT NULL COMMENT '规格JSON，如{"颜色":"红","尺码":"M"}',
  `price`       decimal(12,2)  NOT NULL DEFAULT 0.00 COMMENT '销售价（元）',
  `stock`       int            NOT NULL DEFAULT 0 COMMENT '可售库存（P1简单库存）',
  `status`      char(1)        NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by`   varchar(64)    DEFAULT '' COMMENT '创建者',
  `create_time` datetime       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)    DEFAULT '' COMMENT '更新者',
  `update_time` datetime       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`      varchar(500)   DEFAULT NULL COMMENT '备注',
  `del_flag`    char(1)        NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mall_sku_code` (`sku_code`),
  KEY `idx_mall_sku_spu` (`spu_id`),
  KEY `idx_mall_sku_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城SKU';

CREATE TABLE IF NOT EXISTS `mall_spu_image` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spu_id`      bigint       NOT NULL COMMENT 'SPU ID',
  `url`         varchar(512) NOT NULL COMMENT '图片URL',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_mall_spu_image_spu` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城SPU图片';
