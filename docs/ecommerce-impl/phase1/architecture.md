# Phase 1 — 架构图

> 图均为 Mermaid，可在 GitHub / IDE 预览。实现时以本图为边界；超出范围请先改总览与任务拆分。

## 1. 逻辑架构（模块化单体）

```mermaid
flowchart TB
  subgraph Client["客户端"]
    AdminUI["管理后台<br/>Vue3 Element Plus<br/>/mall/admin views"]
    MallUI["C 端商城<br/>同 SPA /mall/**"]
  end

  subgraph Gateway["接入（现有）"]
    Vite["Vite / Nginx<br/>/dev-api → :8080"]
    Security["Spring Security + JWT<br/>@Anonymous 公开接口"]
  end

  subgraph App["ruoyi-admin 启动进程"]
    subgraph Existing["已有能力"]
      System["ruoyi-system<br/>SysUser / RBAC"]
      Quartz["ruoyi-quartz<br/>超时关单任务"]
      Upload["上传<br/>FileStorage / OSS / common"]
      Blog["ruoyi-blog（不动）"]
      Wechat["ruoyi-wechat<br/>公众号（不接支付）"]
    end

    subgraph Mall["Phase 1 新增"]
      Product["ruoyi-mall-product<br/>类目/品牌/SPU/SKU"]
      Trade["ruoyi-mall-trade<br/>购物车/订单/地址"]
      Payment["ruoyi-mall-payment<br/>支付单 + 渠道 SPI"]
    end
  end

  subgraph Data["数据"]
    MySQL[("MySQL nova_mall")]
    Redis[("Redis<br/>Token/验证码")]
  end

  subgraph External["外部"]
    PayCh["微信支付 / 支付宝<br/>或 MockPay"]
  end

  AdminUI --> Vite
  MallUI --> Vite
  Vite --> Security
  Security --> Product
  Security --> Trade
  Security --> Payment
  Security --> System
  Product --> MySQL
  Trade --> MySQL
  Payment --> MySQL
  Payment --> PayCh
  Trade --> Product
  Payment --> Trade
  Quartz --> Trade
  AdminUI -.-> Upload
  MallUI -.-> Upload
  System --> Redis
```

## 2. 部署与工程边界

```mermaid
flowchart LR
  subgraph Repo["同一仓库"]
    FE["frontend/<br/>admin + /blog + /mall"]
    BE["backend/<br/>多模块 JAR"]
    SQLDocs["docs/ecommerce-impl/phase1/sql<br/>→ 落地时同步到 sql/"]
  end

  FE -->|"/dev-api 或 /prod-api"| BE
  BE --> SQLDocs
```

要点：

- C 端与后台仍是**一个前端工程**，路由分区，不新建独立商城仓库（P1）。
- 支付逻辑只在 `ruoyi-mall-payment`，**不**写入 `ruoyi-wechat`。
- 库存 P1 仅为 `mall_sku.stock` 条件更新，无独立库存服务。

## 3. C 端下单支付主链路

```mermaid
sequenceDiagram
  autonumber
  actor U as 用户
  participant UI as /mall 前端
  participant T as mall-trade
  participant P as mall-product
  participant Pay as mall-payment
  participant Ch as 支付渠道
  participant DB as MySQL

  U->>UI: 加购 / 结算
  UI->>T: POST /mall/orders
  T->>P: 校验 SKU 上架 + 锁库存
  P->>DB: UPDATE stock WHERE stock>=n
  T->>DB: 写 order + order_item 快照 + order_log
  T-->>UI: orderId, PENDING_PAY

  U->>UI: 发起支付
  UI->>Pay: POST /mall/payments
  Pay->>DB: 写 payment_order
  Pay->>Ch: 预下单
  Ch-->>UI: 调起参数
  U->>Ch: 完成支付
  Ch->>Pay: 异步回调 /public/mall/payments/notify/{channel}
  Pay->>Pay: 验签 + 幂等
  Pay->>DB: payment SUCCESS
  Pay->>T: 订单 PENDING_PAY → PAID
  T->>DB: order_log
```

## 4. 订单状态机（P1）

```mermaid
stateDiagram-v2
  [*] --> PENDING_PAY: 下单成功（已扣库存）
  PENDING_PAY --> PAID: 支付回调成功
  PENDING_PAY --> CANCELLED: 用户取消 / 超时 Quartz
  PAID --> SHIPPED: 运营手工发货（可选）
  SHIPPED --> COMPLETED: 确认收货 / 运营完成（可选）
  CANCELLED --> [*]
  COMPLETED --> [*]

  note right of PENDING_PAY
    取消或超时：回滚 mall_sku.stock
  end note
  note right of PAID
    P1 无物流单表；
    发货仅改状态占位
  end note
```

## 5. 支付单状态与订单关系

```mermaid
flowchart LR
  O["mall_order<br/>PENDING_PAY"] -->|创建| PO["mall_payment_order<br/>INIT/PAYING"]
  PO -->|回调成功幂等| POS["mall_payment_order<br/>SUCCESS"]
  POS -->|驱动| OP["mall_order<br/>PAID"]
  PO -->|失败/关闭| POF["CLOSED / FAILED"]
```

规则：

- 一笔业务订单可对应多次支付尝试（多次 `pay_no`），但**仅一笔**可成功入账。
- 回调金额必须等于订单 `pay_amount`。

## 6. API 分区（示意）

| 分区 | 前缀示例 | 鉴权 |
|---|---|---|
| 公开浏览 | `/public/mall/spus` | `@Anonymous` |
| C 端登录 | `/mall/cart`、`/mall/orders`、`/mall/address`、`/mall/payments` | JWT（SysUser） |
| 支付回调 | `/public/mall/payments/notify/{channel}` | 匿名 + 渠道验签 |
| 运营后台 | `/mall/category`、`/mall/spu`、`/mall/admin/orders` | JWT + `mall:*` 权限 |

## 7. 与后续 Phase 的扩展点

```mermaid
flowchart TB
  P1["Phase 1<br/>sku.stock / order / payment"]
  P2["Phase 2<br/>inventory 预占、物流、售后、评价"]
  P3["Phase 3<br/>营销、ES、CMS、消息"]
  P4["Phase 4<br/>商家、风控、结算…"]

  P1 --> P2
  P2 --> P3
  P3 --> P4
```

P1 表设计已预留：`order` 状态枚举可扩展；`payment` 与订单解耦便于后续退款单；SKU `stock` 在 P2 可降级为展示缓存或改为由库存中心回写。
