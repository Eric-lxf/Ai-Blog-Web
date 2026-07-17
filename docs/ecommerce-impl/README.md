# NovaMall 电商分期实现资料

本目录存放**准备落地/已落地**各 Phase 的架构图、E-R 图与数据库脚本，与改造总览、任务拆分文档配合使用。

| 文档 | 用途 |
|---|---|
| [`../ecommerce-platform-transformation-plan.md`](../ecommerce-platform-transformation-plan.md) | 分期总览与验收标准 |
| [`../superpowers/plans/2026-07-17-phase1-mall-mvp.md`](../superpowers/plans/2026-07-17-phase1-mall-mvp.md) | Phase 1 任务拆分 |
| **本目录** | 实现前/实现中的架构与数据设计资产 |

## 目录约定

```text
docs/ecommerce-impl/
├── README.md                 ← 本文件
├── phase1/                   ← 交易最小闭环（当前可开工资料）
│   ├── README.md
│   ├── architecture.md       ← 架构图
│   ├── er-diagram.md         ← E-R 图
│   └── sql/                  ← DDL / 菜单种子 / 演示数据
├── phase2/                   ← 履约闭环（占位，实现时补充）
├── phase3/                   ← 增长运营（占位）
└── phase4/                   ← 平台化（占位）
```

每个 `phaseN/` 建议至少包含：

| 文件/目录 | 说明 |
|---|---|
| `README.md` | 本阶段范围、表清单、执行顺序、与代码模块映射 |
| `architecture.md` | 模块/调用/状态机等架构图（Mermaid） |
| `er-diagram.md` | E-R 图（Mermaid `erDiagram`） |
| `sql/` | 可执行 MySQL 脚本；实现时再同步到仓库根目录 `sql/` |

## 与根目录 `sql/` 的关系

- **设计源**：本目录 `phaseN/sql/` 为对应阶段的权威 DDL 草稿/定稿。
- **运行初始化**：业务代码落地时，将脚本按命名约定复制/合并到仓库根 `sql/`（如 `mall_product_schema.sql`），并更新根 `README.md` 执行顺序。
- 未进入实现的 Phase，**只**在本目录维护，避免污染线上初始化清单。

## 阶段状态

| Phase | 主题 | 资料状态 |
|---|---|---|
| [Phase 1](./phase1/) | 商品 + 购物车/订单 + 支付 + 地址 + C 端 `/mall` | ✅ 架构图 / E-R / SQL 已就绪 |
| [Phase 2](./phase2/) | 库存中心、物流、售后、评价 | ⏳ 占位 |
| [Phase 3](./phase3/) | 营销、搜索、CMS、消息统一 | ⏳ 占位 |
| [Phase 4](./phase4/) | 商家、风控、结算、数据中台等 | ⏳ 占位 |
