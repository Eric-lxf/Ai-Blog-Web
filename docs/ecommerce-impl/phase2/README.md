# Phase 2 — 完整履约闭环（占位）

**状态**：⏳ 未开始  
**触发条件**：Phase 1 交易闭环已验收

## 计划范围（摘自总览）

- `ruoyi-mall-inventory`：多仓、预占/扣减、防超卖
- `ruoyi-mall-logistics`：运费、发货单、轨迹
- `ruoyi-mall-aftersale`：退货退款换货
- `ruoyi-mall-review`：评价晒图

## 本目录待补充（实现前）

| 文件 | 说明 |
|---|---|
| `architecture.md` | 库存预占时序、售后状态机 |
| `er-diagram.md` | inventory / shipment / aftersale / review |
| `sql/` | `mall_inventory_*`、`mall_shipment_*`、`mall_aftersale_*`、`mall_review_*` |

与 Phase 1 接点：弱化或接管 `mall_sku.stock`；`PAID → SHIPPED` 改为写物流单；支付单旁挂退款单。
