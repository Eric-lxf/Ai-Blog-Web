SET NAMES utf8mb4;
USE nova_mall;

-- Phase 1: 可选演示数据（本地联调）。可重复执行：使用固定 ID + INSERT IGNORE

INSERT IGNORE INTO `mall_category` (`id`, `parent_id`, `name`, `sort`, `status`, `create_by`) VALUES
(1, 0, '演示类目', 1, '0', 'admin'),
(2, 1, '数码配件', 1, '0', 'admin');

INSERT IGNORE INTO `mall_brand` (`id`, `name`, `logo`, `sort`, `status`, `create_by`) VALUES
(1, 'NovaDemo', NULL, 1, '0', 'admin');

INSERT IGNORE INTO `mall_spu` (`id`, `category_id`, `brand_id`, `name`, `subtitle`, `main_image`, `detail_html`, `status`, `sort`, `create_by`) VALUES
(1, 2, 1, '演示商品：无线耳机', 'Phase1联调用', NULL, '<p>演示详情</p>', 'ON', 1, 'admin');

INSERT IGNORE INTO `mall_sku` (`id`, `spu_id`, `sku_code`, `specs_json`, `price`, `stock`, `status`, `create_by`) VALUES
(1, 1, 'DEMO-SKU-BLACK', '{"颜色":"黑色"}', 99.00, 100, '0', 'admin'),
(2, 1, 'DEMO-SKU-WHITE', '{"颜色":"白色"}', 109.00, 50, '0', 'admin');

INSERT IGNORE INTO `mall_spu_image` (`id`, `spu_id`, `url`, `sort`) VALUES
(1, 1, '/uploads/demo/earphone.png', 1);
