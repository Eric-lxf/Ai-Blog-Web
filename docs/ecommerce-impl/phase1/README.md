# Phase 1 — 交易最小闭环：实现资料

**状态**：设计就绪（待编码）  
**对应计划**：[任务拆分](../../superpowers/plans/2026-07-17-phase1-mall-mvp.md) · [总览](../../ecommerce-platform-transformation-plan.md)

## 本目录内容

| 文件 | 说明 |
|---|---|
| [architecture.md](./architecture.md) | 模块架构、请求链路、订单/支付状态机 |
| [er-diagram.md](./er-diagram.md) | E-R 图与表关系说明 |
| [sql/](./sql/) | MySQL DDL、菜单种子、演示数据 |

## 范围（P1 做 / 不做）

**做：** 类目/品牌/SPU/SKU、简单库存字段、购物车、订单、支付单、收货地址、后台菜单、C 端 `/mall` 所需数据模型。

**不做：** 多仓库存/预占表、物流单、售后、评价、优惠券、ES、独立 `mall_member` 表（默认身份方案 A：复用 `sys_user`）。

## 代码模块映射（实现时）

| 表前缀 / 能力 | Maven 模块 |
|---|---|
| `mall_category` / `mall_brand` / `mall_spu*` / `mall_sku` | `ruoyi-mall-product` |
| `mall_cart` / `mall_order*` | `ruoyi-mall-trade` |
| `mall_payment_order` | `ruoyi-mall-payment` |
| `mall_address` | `ruoyi-mall-trade` 或 product 旁路均可，建议放 trade |
| 菜单 `sys_menu` 3000+ | `sql/mall_menu_seed.sql` |

## SQL 执行顺序

在已初始化 `nova_mall`（含 `ry_base.sql` 等）的库上，按序执行：

1. `sql/01_mall_category_brand_schema.sql`
2. `sql/02_mall_product_schema.sql`
3. `sql/03_mall_address_schema.sql`
4. `sql/04_mall_cart_order_schema.sql`
5. `sql/05_mall_payment_schema.sql`
6. `sql/06_mall_menu_seed.sql`
7. `sql/07_mall_demo_seed.sql`（可选，本地联调）

详见 [sql/README.md](./sql/README.md)。
