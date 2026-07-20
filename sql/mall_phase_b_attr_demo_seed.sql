SET NAMES utf8mb4;
USE nova_mall;

-- Phase B: 可选演示数据 — 为演示叶子类目（id=2 数码配件）绑定颜色/尺码 SALE 属性
-- 可重复执行：固定 ID + INSERT IGNORE
-- 依赖: mall_phase_b_migrate_front_category.sql, mall_demo_seed.sql

INSERT IGNORE INTO `mall_attr` (`id`, `name`, `input_type`, `status`, `sort`, `create_by`) VALUES
(1, '颜色', 'select', '0', 1, 'admin'),
(2, '尺码', 'select', '0', 2, 'admin');

INSERT IGNORE INTO `mall_attr_option` (`id`, `attr_id`, `value`, `sort`, `status`) VALUES
(1, 1, '黑色', 1, '0'),
(2, 1, '白色', 2, '0'),
(3, 1, '红色', 3, '0'),
(4, 2, 'S', 1, '0'),
(5, 2, 'M', 2, '0'),
(6, 2, 'L', 3, '0');

INSERT IGNORE INTO `mall_category_attr` (`id`, `category_id`, `attr_id`, `attr_type`, `required`, `sort`) VALUES
(1, 2, 1, 'SALE', '1', 1),
(2, 2, 2, 'SALE', '1', 2);
