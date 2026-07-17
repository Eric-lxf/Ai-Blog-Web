# 电商平台化改造计划：从 NovaMall（博客基座）到完整 C 端购物平台

**状态**：Draft（已按现有代码库核对修订）  
**作者**：Cloud Agent（架构 & 产品视角）  
**最后更新**：2026-07-17  
**范围**：后台系统全景梳理 + **基于当前仓库真实能力**的改造映射 + 分期路线图 + Phase 1 任务索引  
**配套文档**：
- Phase 1 任务拆分：[`docs/superpowers/plans/2026-07-17-phase1-mall-mvp.md`](./superpowers/plans/2026-07-17-phase1-mall-mvp.md)
- **分期实现资料（架构图 / E-R / SQL）**：[`docs/ecommerce-impl/`](./ecommerce-impl/README.md)（Phase 1 已就绪；Phase 2–4 占位，后续实现时补充）

---

## 1. 背景与现状（代码事实）

### 1.1 当前项目是什么

**NovaMall** 是 **RuoYi-Vue 3.9.2 管理后台骨架 + 博客业务模块 + 微信公众号模块**，正在向电商平台演进。项目标识、库名已统一为 NovaMall / `nova_mall`（见 `docs/novamall-rename-notes.md`）。它**还不是**电商系统。

| 层 | 现状 | 证据 |
|---|---|---|
| 后台基座 | RBAC、菜单、字典、日志、定时任务、代码生成器 | `backend/ruoyi-system`、`ruoyi-framework`、`ruoyi-quartz`、`ruoyi-generator` |
| 业务模块 | 博客（文章/分类/标签/评论/审核/通知/账单识别/AI 写作） | `backend/ruoyi-blog/.../controller`：`BlogArticleController`、`BlogCommentController`、`BlogBillController` 等 |
| 渠道模块 | **微信公众号**（账号/粉丝/素材/菜单/模板消息/客服等） | `backend/ruoyi-wechat`；**不含微信支付、不含小程序登录** |
| AI 能力 | 多 Provider 配置、AI 写作/优化/流式对话 | `AiProviderController`、`AiWriteController`、`AiOptimizeController`、`AiController` |
| 前端 | 单 SPA：管理后台 + 公开博客 `/blog` | `frontend/src/views/system`、`frontend/src/views/public/blog`；路由白名单含 `/blog` |
| 数据 | MySQL `nova_mall` + Redis（Token/验证码） | `sql/00-init-db.sql`、`application-druid.yml` |
| 上传 | 通用 `/common/upload` + 博客图床/文件库（本地或 OSS） | `CommonController`、`FileUploadController`、`FileStorageService` / `OssFileStorageServiceImpl` |
| C 端账号 | **无独立会员体系**；公开接口多为 `@Anonymous`；评论可选关联 `SysUser` 或游客 | `SecurityConfig`、`PublicCommentController`、`BlogCommentServiceImpl` |

**关键结论**：仓库内**没有**商品 / 库存 / 订单 / 购物车 / 支付 / 物流 / 营销表或代码。改造是在现有基座上**新增交易域模块**，并增加 C 端商城前台；不是给博客「加点功能」。

### 1.2 目标

把项目改造成以**自营为主**的 C 端购物平台（多商家入驻为可选后续），包含：

- C 端购物入口（前台，工作量相对小）
- 完整后台交易与运营能力（商品、交易、库存、支付、营销、物流、售后……工作量集中处）

### 1.3 相对旧版计划的修订要点

| 旧表述 | 代码核对后的事实 | 对本计划的影响 |
|---|---|---|
| 项目名 Ai-Blog-Web / 库 `ai_blog` | 已是 NovaMall / `nova_mall` | 全文按 NovaMall 表述 |
| 「复用 ruoyi-wechat 做微信支付/小程序商城」 | `ruoyi-wechat` 仅为公众号能力；对比文档也标明支付/小程序未做 | Phase 1 支付与小程序**不能**按「已有基础」估算；支付通道全新对接 |
| 「会员 = 现有用户体系 + 地址簿」 | 仅有后台 `SysUser`，无 C 端会员表 | Phase 1 必须先定 C 端身份模型（见第 9 节） |
| C 端独立子应用/独立域名 | 现有公开前台在同一 `frontend/` SPA 的 `/blog` | Phase 1 **默认**在同 SPA 增加 `/mall/**`，降低发布与鉴权成本；独立域名留到流量/SEO 需要时再拆 |
| 库存中心在 Phase 2 | Phase 1 验收又要求「库存」字段 | Phase 1 仅在 SKU 上维护**简单可售数量**；真正预占/多仓/防超卖放 Phase 2 |

---

## 2. 成熟电商平台的后台系统全景

参考淘宝/京东/Shopify/SHEIN/拼多多的公开架构与行业通识，完整电商后台约 **4 大域、18 个子系统**。C 端网站只是能力出口。

### 2.1 交易核心域

| 系统 | 核心职责 | 典型实体 | 对标 |
|---|---|---|---|
| 用户/会员中心 | 注册登录、地址、等级、积分、第三方登录 | User、Address、MemberLevel | 淘宝会员、京东 PLUS |
| 商品中心（PIM） | 类目、品牌、SPU/SKU、属性、图文、审核上下架 | Category、Brand、SPU、SKU | 京东商品中心 |
| 购物车/订单（OMS） | 加购、下单、状态机、取消 | Cart、Order、OrderItem | 淘宝交易 |
| 库存中心 | 多仓、预占/扣减、防超卖 | Inventory、StockLock | 京东仓配 |
| 支付中心 | 收银台、渠道网关、回调、对账、退款 | PaymentOrder、Channel | 微信/支付宝网关 |
| 物流中心（TMS） | 运费、发货、轨迹 | Shipment、Carrier | 菜鸟/京东物流 |
| 售后中心 | 退货退款换货 | AfterSaleOrder、RefundOrder | 淘宝售后 |

### 2.2 商品与供应链域

| 系统 | 核心职责 | 说明 |
|---|---|---|
| 供应链/采购 | 供应商、采购、入库、成本 | 自营后期可加 |
| 商家/店铺中心 | 入驻、多店商品、分账 | **仅平台型需要** |
| WMS | 拣货打包出库 | Phase 2+ |

### 2.3 营销与内容域

| 系统 | 核心职责 |
|---|---|
| 营销中心 | 券、满减、限时、秒杀、拼团 |
| 搜索与推荐 | 搜索引擎、推荐 |
| CMS | 首页楼层、Banner、专题 |
| 评价/UGC | 评价晒图（可复用评论/敏感词思路） |
| 消息中心 | 站内信/短信/邮件/微信模板消息统一出口 |

### 2.4 支撑与治理域

| 系统 | 现状 |
|---|---|
| 风控 / 客服 / 财务结算 / 数据中台 / 开放平台 | 均未建设，属 Phase 3–4 |
| 权限与运营后台基础设施 | **已具备（RuoYi）** |

**小结**：C 端只覆盖展示、加购、下单、支付跳转、订单查询等读多写少交互；库存、履约、营销、风控、结算大多在后台。

---

## 3. 现状 → 目标：系统映射与改造方式

| 目标系统 | 处理方式 | 说明 |
|---|---|---|
| RBAC / 日志 / 字典 / Quartz / 代码生成 | **直接复用** | 现有基座原样保留 |
| 文件上传 | **复用** | 优先复用 `FileStorageService`（本地/OSS）与 `/common/upload`；商品图走同一存储，业务表只存 URL |
| AI | **复用并延伸**（非 Phase 1 阻塞） | 商品文案辅助、后续智能客服等 |
| `ruoyi-wechat` | **复用公众号能力**；支付/小程序**新建** | 发货通知等可接模板消息；微信支付 SDK/商户配置独立于公众号模块 |
| `ruoyi-blog` | **保留**为内容/SEO 子系统 | 不与商品模块强行合并；菜单 ID 段继续占用 2000–2999 一带 |
| 评论 / 敏感词 / 通知 | **Phase 2+ 再抽取公共能力** | Phase 1 不抽公共库，避免范围膨胀 |
| 商品 / 订单 / 支付 / 地址 / 简单库存字段 | **Phase 1 新建** | 见配套 Phase 1 计划 |
| 库存中心 / 物流 / 售后 / 评价 / 营销 / 搜索 / 商家 / 风控 | **Phase 2–4 新建** | |
| C 端商城前台 | **新建 `/mall` 路由与公开 API** | 与 `/blog` 并列，同 SPA |

### 3.1 新增模块时必须同步改动的工程点（易漏）

按现有 `ruoyi-blog` / `ruoyi-wechat` 接入方式，每个新 Maven 模块至少要：

1. `backend/pom.xml` modules + `ruoyi-admin/pom.xml` 依赖  
2. 模块内 `@MapperScan`（参照 `BlogMybatisPlusConfig` / `WechatMybatisPlusConfig`）  
3. `backend/Dockerfile` 复制新模块目录  
4. `application.yml` 中 `springdoc` 的 `packages-to-scan` 追加新 controller 包  
5. `sql/` 增加 schema + `mall_menu_seed.sql`（建议菜单 ID **≥ 3000**，避开博客/公众号段）  
6. README / 初始化 SQL 执行顺序补充  

---

## 4. 分期路线图（按技术范围，不按日历）

> 原则：先打通最小闭环，再履约，再增长，最后平台化。用 **S/M/L/XL** 刻画复杂度，不给天数估算。

### 🟢 Phase 1 — 交易最小闭环（MVP）

**目标**：C 端可浏览商品、加购、下单、完成（沙箱/测试）支付；运营可管商品与订单。

**规模：L**（约 3 个后端模块 + 会员地址扩展 + C 端页面；约 10+ 张表；1 类外部依赖——支付渠道；无 ES/MQ；资金流存在但无高并发超卖设计）

| 交付块 | 核心内容 |
|---|---|
| `ruoyi-mall-product` | 类目树、品牌、SPU/SKU、规格、图片、上下架；SKU 上**简单库存数字** |
| `ruoyi-mall-trade`（订单+购物车，一期可同模块） | 购物车、下单、订单状态机、订单项快照、订单日志 |
| `ruoyi-mall-payment` | 统一支付单抽象；微信/支付宝至少打通 **1 条**可回调链路（可用沙箱） |
| 会员/地址 | C 端身份 + `mall_address` |
| C 端 `/mall` | 首页、列表、详情、购物车、结算、支付结果、订单列表/详情 |
| Quartz | 待支付超时自动取消 |

**验收标准**：

- [ ] 后台可创建并上架含 ≥1 个 SKU 的商品，列表可见价格 / 简单库存 / 上下架状态  
- [ ] 已登录 C 端用户可加购并下单，生成「待支付」订单，金额 = Σ(快照单价 × 数量)  
- [ ] 支付渠道异步回调后订单 →「待发货」；重复回调幂等  
- [ ] 订单详情展示商品快照（后续改商品不影响历史订单）  
- [ ] 超时未支付订单由定时任务关闭为「已取消」  
- [ ] 后台可按状态/时间筛订单，可看状态流转日志  

**详细任务拆分**：见 [`2026-07-17-phase1-mall-mvp.md`](./superpowers/plans/2026-07-17-phase1-mall-mvp.md)（**本文不展开实现步骤；先拆任务、不编码**）。

**架构图 / E-R / DDL**：见 [`docs/ecommerce-impl/phase1/`](./ecommerce-impl/phase1/README.md)。

### 🟡 Phase 2 — 完整履约闭环

**规模：XL**

| 模块 | 内容 |
|---|---|
| `ruoyi-mall-inventory` | 多仓、预占/支付扣减、Redis+Lua 防超卖、超时释放 |
| `ruoyi-mall-logistics` | 运费模板、发货单、快递查询、轨迹 |
| `ruoyi-mall-aftersale` | 退货退款换货 |
| `ruoyi-mall-review` | 评价晒图；复用敏感词/审核思路 |

**验收要点**：并发不超卖、超时释库存、发货可见轨迹、售后驱动退款、评价过敏感词。

### 🔵 Phase 3 — 增长与运营

**规模：L**（引入 ES；秒杀为可选高风险子项）

营销中心、Elasticsearch 搜索、CMS 楼层/Banner、消息出口统一化。

### ⚪ Phase 4 — 平台化与规模化（按需）

商家中心、风控、财务分账、数据中台、开放 API、分库分表/MQ/Seata——各自独立立项，避免捆绑。

### ❌ 本次不做（Non-goals）

| 事项 | 原因 |
|---|---|
| 多仓智能分仓算法 | 无履约数据，一期无 ROI |
| 多币种/多语言跨境 | 无明确跨境需求则过度设计 |
| 直播电商 | 独立流量体系 |
| 供应链金融 | 战略级，单独立项 |
| Phase 1 引入 ES / MQ / Seata | 过早复杂化 |
| Phase 1 抽公共「评论中台」 | 与 MVP 无关，延后 |

---

## 5. 关键新增系统设计要点

### 5.1 商品中心

- 表：`mall_category`、`mall_brand`、`mall_spu`、`mall_sku`、`mall_product_attr*`、`mall_spu_image`  
- Phase 1 SKU 含 `price`、`stock`（简单可售数）；**不做**锁库存表  
- 图片 URL 复用现有上传能力  

### 5.2 订单中心

- 表：`mall_cart`、`mall_order`、`mall_order_item`、`mall_order_log`  
- 状态机 Phase 1：`PENDING_PAY → PAID(待发货) / CANCELLED → ...`（发货/完成可在后台手工推进到「已发货/已完成」占位，完整物流在 Phase 2）  
- 超时关闭：复用 `ruoyi-quartz` 扫描  

### 5.3 支付中心

- 表：`mall_payment_order`（与业务订单解耦）、可选 `mall_refund_order`（Phase 1 可只建表不做完整退款 UI）  
- **统一支付网关接口**，渠道可插拔；**不**把支付逻辑写进 `ruoyi-wechat`  
- 回调接口必须 `@Anonymous` + 验签 + 幂等  

### 5.4 库存（Phase 1 vs 2）

- Phase 1：下单时校验并扣减 `mall_sku.stock`（单表事务即可）；取消/超时回滚库存  
- Phase 2：独立库存中心 + Redis 预扣 + 对账  

### 5.5 营销（Phase 3）

- 券模板/领取记录；秒杀独立链路，不冲击主交易  

---

## 6. C 端网站（前台）建设方案

| 维度 | Phase 1 建议 |
|---|---|
| 部署形态 | **同 SPA**：`/mall`、`/mall/product/:id` 等；布局可新建 `MallPublicLayout`，对齐 `BlogPublicLayout` 经验 |
| 技术栈 | 继续 Vue3 + 现有 `request.js`；SEO 硬需求出现再评估 Nuxt SSR |
| 页面 | 首页 → 列表/类目 → 详情 → 购物车 → 结算 → 支付结果 → 订单中心 → 地址管理 |
| API | 公开/C 端前缀建议 `/public/mall/**` 与 `/mall/**`（需登录）；后台运营 `/mall/admin/**` 或 `/product` 等与博客 `blog:*` 权限风格一致 |
| 代理 | 继续走 `/dev-api` → `8080`，无需新代理前缀 |
| 小程序 | **非 Phase 1**；待微信支付与会员模型稳定后再开渠道 |

---

## 7. 数据库 Schema 规划

**权威设计稿**（含可执行 DDL）放在 [`docs/ecommerce-impl/`](./ecommerce-impl/README.md)；业务代码落地时再同步到仓库根目录 `sql/`。

| 阶段 | 设计目录 | 根目录 `sql/` 落地名（实现时） |
|---|---|---|
| P1 | [`phase1/sql/`](./ecommerce-impl/phase1/sql/) 已含 01–07 脚本 | `mall_category_brand_schema.sql` 等 |
| P2 | `phase2/sql/`（占位） | `mall_inventory_*` / `mall_aftersale_*` / `mall_review_*` |
| P3 | `phase3/sql/`（占位） | `mall_marketing_*` / CMS 相关 |
| P4 | `phase4/sql/`（占位） | 按子项拆分 |

P1 默认不建 `mall_member`（身份方案 A 复用 `sys_user`）；若改选方案 B，在 `phase1/sql/` 追加会员表脚本。

---

## 8. 技术架构关键决策（简版 ADR）

| 决策点 | 建议 | 理由 |
|---|---|---|
| 单体 vs 微服务 | 继续模块化单体，新增 `ruoyi-mall-*` | 与 `ruoyi-blog`/`ruoyi-wechat` 一致；当前流量不值得微服务运维成本 |
| 交易模块切分 | P1：`product` + `trade`(cart/order) + `payment`；库存后置 | 减少空模块；边界仍清晰 |
| C 端前台 | 同 SPA `/mall` | 复用构建、代理、登录态基础设施 |
| 库存一致性 P1 | MySQL 行锁/条件更新 `stock` | 够用；P2 再上 Redis |
| 异步 | P1 用 Quartz；P3/4 视量引入 MQ | 避免过早中间件 |
| 搜索 | P3 引入 ES | LIKE 无法支撑商品搜索 |
| 支付网关 | 独立 `ruoyi-mall-payment`，渠道 SPI | 不污染 wechat 公众号模块 |

---

## 9. 启动 Phase 1 前必须拍板的问题

1. **自营 vs 平台型**：是否多商家？决定是否永远不做商家中心。  
2. **C 端身份模型（P1 阻塞）**：  
   - **A**：复用 `SysUser` + 角色（如 `mall_customer`），地址挂 `user_id` —— 实现快，后台账号与买家账号同表，权限要严格隔离。  
   - **B**：新建 `mall_member` + 独立 Token/登录 —— 边界清晰，工作量更大。  
   - **建议默认 A**，除非有明确「买家与运营账号必须分离」合规要求。  
3. **仓储**：单仓即可（P1 无仓表）。  
4. **支付**：境内微信 / 支付宝优先哪一条打通？沙箱是否可验收？  
5. **扣库存时点（P1）**：建议**下单扣减 + 取消/超时回滚**；与 P2「预占」语义对齐但实现更简单。  
6. **营销**：P1 不做券/秒杀。  
7. **博客**：保留作内容引流；菜单与包名不改。  

未拍板前，工程可先做「模块脚手架 + 商品后台只读链路」，但**不应**开工支付与 C 端结算。

---

## 10. 总结

- 真正工程量在后台交易域；C 端是橱窗。  
- 可复用：RBAC、Quartz、上传（含 OSS）、AI、公众号模板消息思路、公开前台路由模式。  
- **不可高估复用**：微信支付、小程序、独立会员——当前仓库均未具备。  
- 节奏：P1 交易闭环 → P2 履约 → P3 增长 → P4 平台化（按需）。  
- **下一步**：评审并锁定第 9 节决策后，按 [`2026-07-17-phase1-mall-mvp.md`](./superpowers/plans/2026-07-17-phase1-mall-mvp.md) 逐任务实现（本文及该拆分文档**仅规划，不含实现**）。
