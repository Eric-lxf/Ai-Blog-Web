# Phase 1 — SQL 脚本说明

## 执行前置

- 库已创建：`sql/00-init-db.sql` → `nova_mall`
- 已执行若依基座：`ry_base.sql`（含 `sys_user` / `sys_menu` / `sys_role_menu`）
- 字符集：`utf8mb4`；会话时区建议 `+08:00`

## 顺序

| 序号 | 文件 | 内容 |
|---|---|---|
| 1 | `01_mall_category_brand_schema.sql` | 类目、品牌 |
| 2 | `02_mall_product_schema.sql` | SPU、SKU、图片 |
| 3 | `03_mall_address_schema.sql` | 收货地址 |
| 4 | `04_mall_cart_order_schema.sql` | 购物车、订单、订单项、订单日志 |
| 5 | `05_mall_payment_schema.sql` | 支付单 |
| 6 | `06_mall_menu_seed.sql` | 后台菜单与权限（menu_id ≥ 3000） |
| 7 | `07_mall_demo_seed.sql` | 可选演示数据 |

示例：

```bash
mysql -uroot -p nova_mall < docs/ecommerce-impl/phase1/sql/01_mall_category_brand_schema.sql
# …按序执行其余文件
```

## 落地到仓库根 `sql/`

实现 Task 1 时，可将本目录脚本复制为例如：

- `sql/mall_category_brand_schema.sql`
- `sql/mall_product_schema.sql`
- …

并在根 `README.md` 初始化清单中登记。未实现前**不必**复制，以免空跑初始化流程。

## 回滚提示

P1 表无历史依赖，开发库可：

```sql
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS mall_payment_order, mall_order_log, mall_order_item, mall_order,
  mall_cart, mall_address, mall_spu_image, mall_sku, mall_spu, mall_brand, mall_category;
SET FOREIGN_KEY_CHECKS = 1;
DELETE FROM sys_role_menu WHERE menu_id >= 3000 AND menu_id < 4000;
DELETE FROM sys_menu WHERE menu_id >= 3000 AND menu_id < 4000;
```
