# Phase 1 — 商城交易最小闭环（MVP）任务拆分

> **For agentic workers:** 本文是任务拆分与边界说明，**先不要实现**。获准开工后，推荐使用 `superpowers:subagent-driven-development` 或 `executing-plans` 按 Task 顺序落地。  
> 总览与分期见 [`docs/ecommerce-platform-transformation-plan.md`](../../ecommerce-platform-transformation-plan.md)。  
> **架构图 / E-R / SQL 设计稿**：[`docs/ecommerce-impl/phase1/`](../../ecommerce-impl/phase1/README.md)。

**Goal:** 运营可管商品与订单；C 端用户可浏览、加购、下单，并经支付回调进入「待发货」。

**Architecture:** 在现有模块化单体上新增 `ruoyi-mall-product` / `ruoyi-mall-trade` / `ruoyi-mall-payment`；C 端同 SPA 增加 `/mall/**`；支付与公众号模块隔离；P1 库存仅为 `mall_sku.stock` 条件更新。

**Tech Stack:** JDK 17、Spring Boot（RuoYi）、MyBatis、MySQL `nova_mall`、Redis（仅既有 Token）、Vue3 + Element Plus、Quartz、微信支付或支付宝（至少 1 条可回调链路）。

## Global Constraints

- 库名：`nova_mall`；时区：`Asia/Shanghai`
- 菜单 ID：`>= 3000`，权限前缀建议 `mall:*`
- 不修改 `ruoyi-blog` / `ruoyi-wechat` 业务语义；支付代码不放进 `ruoyi-wechat`
- P1 **不做**：ES、MQ、多仓、售后 UI、优惠券、秒杀、小程序、独立商城仓库
- 验证方式对齐仓库惯例：`mvn -B -DskipTests package -pl ruoyi-admin -am`、前端 `npm run build:prod`、API/浏览器冒烟
- C 端身份默认方案 **A**（复用 `SysUser` + 角色）；若业务改选 B，先改 Task 0/4 再动交易

---

## 文件与模块地图（落地后应存在）

| 路径 | 职责 |
|---|---|
| `backend/ruoyi-mall-product/` | 类目/品牌/SPU/SKU/上下架，后台 CRUD + 少量公开只读 API |
| `backend/ruoyi-mall-trade/` | 购物车、订单、订单项快照、订单日志、超时取消 Job 业务 |
| `backend/ruoyi-mall-payment/` | 支付单、渠道 SPI、下单/回调/查单 |
| `docs/ecommerce-impl/phase1/sql/*` → 落地时同步 `sql/mall_*.sql` | schema + menu seed（设计稿已就绪） |
| `frontend/src/views/mall/admin/**` | 运营后台商品/订单页 |
| `frontend/src/views/public/mall/**` | C 端商城页 |
| `frontend/src/api/mall/**` | 前后端 API 封装 |

工程接入清单（每个后端模块 Task 都要覆盖）：父 POM modules、`ruoyi-admin` 依赖、模块 `@MapperScan`、`Dockerfile` COPY、`springdoc` packages-to-scan、README SQL 顺序。

---

## Task 依赖关系

```text
T0 决策锁定
 └─► T1 模块脚手架 + SQL 骨架
      ├─► T2 商品中心后端
      │    └─► T3 商品后台前端
      ├─► T4 C 端身份 + 地址
      │    └─► T5 购物车 + 下单（依赖 T2+T4）
      │         ├─► T6 支付中心（依赖 T5）
      │         └─► T7 超时取消（依赖 T5；可与 T6 并行）
      └─► T8 C 端商城前台（依赖 T2 公开 API；结算依赖 T5/T6）
           └─► T9 端到端冒烟与文档收尾（依赖全部）
```

---

### Task 0: 拍板决策（阻塞项清单）

**产出:** 在总览文档第 9 节或本文件下方「决策记录」写入明确选项；无代码。

**必须确认:**

| # | 问题 | 默认建议 | 影响 Task |
|---|---|---|---|
| D1 | C 端身份 A(`SysUser`) / B(`mall_member`) | A | T4、T5、T8 |
| D2 | 首条支付渠道：微信 / 支付宝 | 微信（境内常见） | T6 |
| D3 | 验收是否允许支付沙箱/模拟回调 | 允许（无商户号时用 Mock 渠道 + 验签开关） | T6、T9 |
| D4 | 扣库存时点 | 下单扣减 + 取消回滚 | T5、T7 |
| D5 | P1 是否允许运营手工「发货/完成」改状态 | 允许（无物流单） | T5 后台 |

- [ ] **Step 1:** 业务方确认 D1–D5 并写入「决策记录」
- [ ] **Step 2:** 未确认前，只允许推进 T1–T3（商品只读链路）

---

### Task 1: Maven 模块脚手架 + SQL 骨架 + 菜单号段

**Files:**
- Create: `backend/ruoyi-mall-product/pom.xml` 及标准 `src/main/java/com/ruoyi/mall/product/config/MallProductMybatisPlusConfig.java`（仅 `@MapperScan`）
- Create: `backend/ruoyi-mall-trade/`、`backend/ruoyi-mall-payment/` 同构空模块
- Modify: `backend/pom.xml`、`backend/ruoyi-admin/pom.xml`、`backend/Dockerfile`
- Modify: `backend/ruoyi-admin/src/main/resources/application.yml`（springdoc 扫描包）
- 以设计稿为准同步到根目录：`docs/ecommerce-impl/phase1/sql/01`–`07` → `sql/mall_*.sql`（字段与菜单已在设计稿写全，勿另起一套）
- Modify: 根 `README.md` SQL 执行顺序

**表结构**：见 [`phase1/er-diagram.md`](../../ecommerce-impl/phase1/er-diagram.md) 与 [`phase1/sql/`](../../ecommerce-impl/phase1/sql/README.md)，勿在本 Task 重复发明字段。

**菜单建议:**

- 根目录「商城」`menu_id=3000`
- 商品、类目、品牌、订单、支付单等子菜单 `3001+`
- 按钮权限 `mall:product:*`、`mall:order:*` 等
- `INSERT IGNORE` + 赋权 `role_id=1`（对齐 `blog_menu_seed.sql`）

**验收:**

- [ ] `mvn -B -DskipTests package -pl ruoyi-admin -am` 通过
- [ ] 空模块可被 Spring 启动加载（无 Controller 也可）
- [ ] SQL 在干净库可执行无冲突

---

### Task 2: 商品中心后端（运营 CRUD + 公开只读）

**依赖:** T1、D 决策不阻塞本 Task

**Files（按博客模块分层习惯）:**
- Create: `com.ruoyi.mall.product.domain.*` / `mapper.*` / `service.*` / `controller.*`
- 运营 API 示例前缀：`/mall/category`、`/mall/brand`、`/mall/spu`、`/mall/sku`
- 公开 API：`/public/mall/spus`、`/public/mall/spus/{id}`（`@Anonymous`，仅 `status=ON`）

**行为要点:**

- SPU 上架前至少 1 个启用 SKU 且 `price>=0`、`stock>=0`
- 删除：逻辑下架优先；物理删需无未完成订单引用（P1 可禁物理删）
- 列表支持类目、状态、名称模糊查询
- 写操作记操作日志（沿用 `@Log`）

**验收:**

- [ ] 后台 JWT 可创建类目/品牌/SPU/SKU 并上架
- [ ] 匿名可拉取已上架 SPU 详情（含 SKU 列表）
- [ ] 未上架商品公开接口不可见

---

### Task 3: 商品运营前端

**依赖:** T2

**Files:**
- Create: `frontend/src/api/mall/product.js` 等
- Create: `frontend/src/views/mall/admin/category/index.vue`、`brand/index.vue`、`spu/index.vue`、`spu/edit.vue`
- 图片上传：复用现有博客/通用上传组件与 `FileStorageService` 返回的 URL

**验收:**

- [ ] 菜单种子生效后，admin 可见「商城」菜单
- [ ] 可完成「新建 SPU → 配 SKU → 上传主图 → 上架」闭环
- [ ] `npm run build:prod` 通过

---

### Task 4: C 端身份 + 收货地址

**依赖:** T1；**阻塞于 D1**

**若 D1=A（默认）:**

- 新增角色/权限：允许注册用户或指定角色访问商城下单接口
- Create: 地址 CRUD ` /mall/address`（登录态，`user_id` 取自 `SecurityUtils`）
- 前端：注册/登录沿用现有 `/register` `/login`；个人中心增加地址页
- **隔离:** 普通买家角色不得访问系统管理菜单（靠角色菜单配置）

**若 D1=B:**

- Create: `mall_member` 表、独立登录签发 Token（需扩展 Token 服务或独立 filter——工作量显著更大，单列子任务）

**验收:**

- [ ] 登录用户可增删改查自己的地址；不能读写他人地址
- [ ] 可设默认地址

---

### Task 5: 购物车 + 下单 + 订单后台

**依赖:** T2、T4；遵守 D4/D5

**Files:** `ruoyi-mall-trade` 全套 domain/mapper/service/controller

**状态机（P1）:**

```text
PENDING_PAY --支付成功--> PAID
PENDING_PAY --用户取消/超时--> CANCELLED（回滚库存）
PAID --运营发货--> SHIPPED          （可选手工）
SHIPPED --确认收货/运营完成--> COMPLETED （可选手工）
```

**下单事务（单库）:**

1. 校验 SKU 上架且库存充足  
2. `UPDATE mall_sku SET stock = stock - ? WHERE id=? AND stock >= ?`（影响行数必须 = 商品行数）  
3. 写 `mall_order` + `mall_order_item`（价格/标题/规格/图片快照）+ `mall_order_log`  
4. 清理对应购物车行  
5. 设置 `expire_time = now + 30min`

**API 建议:**

- C 端：`/mall/cart/**`、`POST /mall/orders`、`GET /mall/orders`、`GET /mall/orders/{id}`、`POST /mall/orders/{id}/cancel`
- 运营：`/mall/admin/orders` 列表筛选、详情、状态流转日志；可选 `ship`/`complete`

**验收:**

- [ ] 加购、改数量、删除购物车项正确
- [ ] 下单金额与快照一致；库存减少
- [ ] 取消订单库存回滚且状态为 `CANCELLED`
- [ ] 后台可见订单与日志

---

### Task 6: 支付中心（渠道可插拔）

**依赖:** T5；遵守 D2/D3

**Files:** `ruoyi-mall-payment`

**设计:**

```text
PaymentGateway (interface)
  createPayment(PaymentCreateCmd): PaymentCreateResult  // 返回调起参数或 redirect url
  parseNotify(channel, headers, body): NotifyResult     // 验签 + 解析
  query(payNo): PayStatus

实现: WechatPayGateway / AlipayGateway / MockPayGateway(D3)
```

**流程:**

1. `POST /mall/payments`：对 `PENDING_PAY` 订单创建 `mall_payment_order`（`pay_no` 唯一）  
2. 调渠道预下单，返回前端调起参数  
3. `POST /public/mall/payments/notify/{channel}`：`@Anonymous`，验签 → 幂等更新支付单 → 驱动订单 `PENDING_PAY→PAID`  
4. 重复通知：已 `SUCCESS` 直接返回成功应答，不改订单两次  

**注意:**

- 不依赖 `ruoyi-wechat` 公众号 SDK  
- 商户密钥走配置/环境变量，禁止提交密钥到仓库  
- 有商户号则接真渠道；否则 Mock + 管理端「模拟支付成功」仅 `dev` profile 可用

**验收:**

- [ ] 一笔订单支付成功后状态 `PAID`，`pay_time` 有值  
- [ ] 同一回调重放 N 次订单仍只成功一次  
- [ ] 支付金额与订单 `pay_amount` 不一致时拒绝入账  

---

### Task 7: 待支付超时自动取消

**依赖:** T5（可与 T6 并行）

**Files:**

- Create: Quartz 任务类（参照现有 `ruoyi-quartz` 任务写法）或 `ruoyi-mall-trade` 内 Job + SQL 种子注册到 `sys_job`
- 扫描：`status=PENDING_PAY AND expire_time < now()`，批量取消并回滚库存（逐单事务，失败记日志）

**验收:**

- [ ] 构造过期订单，任务执行后变为 `CANCELLED` 且库存恢复  
- [ ] 已支付订单不被误取消  

---

### Task 8: C 端商城前台 `/mall`

**依赖:** T2 公开 API；结算依赖 T5/T6；地址依赖 T4

**Files:**

- Modify: `frontend/src/router/index.js` 增加 `/mall` 及子路由  
- Modify: `frontend/src/permission.js` 白名单：浏览类可匿名；购物车/结算/订单需登录（与现网 permission 逻辑对齐）  
- Create: `MallPublicLayout.vue`、`views/public/mall/{home,list,detail,cart,checkout,pay-result,orders,order-detail,address}.vue`  
- Create: `frontend/src/api/mall/public.js`、`cart.js`、`order.js`、`payment.js`

**页面职责（P1 够用即可，避免营销堆砌）:**

| 路由 | 职责 |
|---|---|
| `/mall` | 已上架商品列表/简单推荐 |
| `/mall/list` | 类目/关键词筛选 |
| `/mall/product/:id` | 详情、选 SKU、加购 |
| `/mall/cart` | 购物车 |
| `/mall/checkout` | 选地址、确认、提交订单 |
| `/mall/pay/:orderId` | 调起支付 / Mock 支付 |
| `/mall/orders` | 我的订单 |
| `/mall/address` | 地址簿 |

**验收:**

- [ ] 匿名可逛；未登录加购/结算跳转登录后可回到原目标  
- [ ] 完整走通：逛 → 加购 → 下单 → 支付（或 Mock）→ 订单详情为已支付  
- [ ] 桌面与窄屏可完成主路径（不要求独立 App 级体验）  

---

### Task 9: 端到端冒烟、种子数据与文档收尾

**依赖:** T1–T8

**内容:**

- [ ] SQL：一份 `mall_demo_seed.sql`（可选）含 1 个类目、1 个 SPU、2 个 SKU  
- [ ] 更新根 `README.md`：商城初始化顺序、默认角色说明、支付配置项说明  
- [ ] 对照总览文档 Phase 1 验收清单逐条勾选并记录证据（curl / 截图路径）  
- [ ] `mvn package` + `npm run build:prod` 全绿  
- [ ] 明确列出 **P1 未做** 并指向 Phase 2（物流单、售后、评价、库存中心）  

---

## 决策记录（启动前填写）

| ID | 选择 | 决策人 | 日期 |
|---|---|---|---|
| D1 身份模型 | **A**（复用 SysUser + `mall_customer` 角色） | Cloud Agent（按计划默认） | 2026-07-17 |
| D2 首条支付渠道 | **MOCK 为主** + WECHAT SPI 占位（无商户号可验收） | Cloud Agent（按计划默认 D3） | 2026-07-17 |
| D3 沙箱/Mock 可验收 | **是** | Cloud Agent | 2026-07-17 |
| D4 扣库存时点 | **下单扣减 + 取消/超时回滚** | Cloud Agent | 2026-07-17 |
| D5 手工发货/完成 | **是** | Cloud Agent | 2026-07-17 |

---

## 与总览验收标准的映射

| 总览验收项 | 覆盖 Task |
|---|---|
| 后台商品列表见价格/库存/上下架 | T2、T3 |
| 加购下单待支付且金额正确 | T5、T8 |
| 支付回调待发货 + 幂等 | T6、T9 |
| 订单商品快照 | T5 |
| 30 分钟未支付自动取消 | T7 |
| 后台订单筛选与状态日志 | T5、T3/运营订单页 |

---

## 刻意不做的后续项（勿塞进 P1 PR）

- 从博客抽取公共评论/敏感词组件  
- 把商城拆成独立前端仓库/独立域名  
- 微信支付塞进 `ruoyi-wechat`  
- Redis 扣库存、ES 搜索、优惠券  
- 完整退款/售后状态机  

---

## 执行说明

本文仅拆分任务，**不包含实现**。决策记录填完后，再选择：

1. **Subagent-Driven** — 每 Task 新开子代理，Task 间人工/代理评审  
2. **Inline Execution** — 本会话按 `executing-plans` 批次推进  

未填 D1–D3 前，不要开始 T4 之后的编码。
