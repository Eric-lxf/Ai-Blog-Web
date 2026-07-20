SET NAMES utf8mb4;
USE nova_mall;

-- Phase B: 一次性迁移 mall_category -> mall_front_category + 1:1 rel
-- 可重复执行：若 mall_front_category 已有数据则整段跳过
-- 依赖: mall_attr_front_category_schema.sql；需在 mall_category 有数据后执行

SET @do_migrate := (SELECT COUNT(*) = 0 FROM mall_front_category);

DROP TEMPORARY TABLE IF EXISTS tmp_mall_front_cat_map;
CREATE TEMPORARY TABLE tmp_mall_front_cat_map (
  back_id  BIGINT NOT NULL PRIMARY KEY,
  front_id BIGINT NOT NULL
);

-- 按 parent 层级逐批插入（最多 8 层，覆盖常见类目树深度）
-- Pass 1: 根节点
INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT 0, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
WHERE @do_migrate = 1
  AND c.parent_id = 0
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN mall_front_category f
  ON f.name = c.name
 AND f.sort = c.sort
 AND f.parent_id = 0
WHERE @do_migrate = 1
  AND c.parent_id = 0
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

-- Pass 2–8: 子节点（父级须已映射）
INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO mall_front_category (parent_id, name, sort, status, icon, create_by, create_time, update_by, update_time, remark)
SELECT pm.front_id, c.name, c.sort, c.status, c.icon, c.create_by, c.create_time, c.update_by, c.update_time, c.remark
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

INSERT INTO tmp_mall_front_cat_map (back_id, front_id)
SELECT c.id, f.id
FROM mall_category c
INNER JOIN tmp_mall_front_cat_map pm ON pm.back_id = c.parent_id
INNER JOIN mall_front_category f
  ON f.parent_id = pm.front_id
 AND f.name = c.name
 AND f.sort = c.sort
WHERE @do_migrate = 1
  AND NOT EXISTS (SELECT 1 FROM tmp_mall_front_cat_map m WHERE m.back_id = c.id);

-- 1:1 映射 rel
INSERT IGNORE INTO mall_front_category_rel (front_id, back_category_id)
SELECT m.front_id, m.back_id
FROM tmp_mall_front_cat_map m
WHERE @do_migrate = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_mall_front_cat_map;
