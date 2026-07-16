# 操作日志全覆盖设计

日期：2026-07-16  
状态：草案（待确认）  
分支：`cursor/operlog-coverage-design-a3cc`

## 1. 问题陈述

我们要回答两件事：

1. 现有「操作日志」是怎么设计的？
2. 若目标是「所有操作都有操作日志」，应该怎么做？

当前系统沿用 RuoYi 的 `@Log` 注解驱动方案：只有显式标注的 Controller 方法才会写入 `sys_oper_log`。若依自带的系统管理、监控、定时任务、代码生成已基本覆盖写操作；但本仓库业务模块 `ruoyi-blog`、`ruoyi-wechat` **零 `@Log`**，文章发布、AI 配置、微信推送等关键写操作在操作日志里完全不可见。

「所有操作都记日志」若按字面执行（含全部 GET 列表、公开接口、埋点），会导致日志量爆炸、查询噪音、隐私与成本问题。因此本设计先澄清「操作」的边界，再给出可落地的覆盖策略。

---

## 2. 现状：现有设计方式

### 2.1 架构链路

```
Controller 方法上的 @Log
        ↓
LogAspect（AOP：@Before / @AfterReturning / @AfterThrowing）
        ↓
组装 SysOperLog（操作人、IP、URL、参数、结果、耗时、状态）
        ↓
AsyncManager → AsyncFactory.recordOper()
        ↓
ISysOperLogService.insertOperlog() → sys_oper_log
        ↓
监控页面「系统监控 → 操作日志」查询 / 导出 / 删除 / 清空
```

登录/登出/注册 **不走 `@Log`**，而是直接调用 `AsyncFactory.recordLogininfor()`，写入独立表 `sys_logininfor`。操作日志与登录日志是两套体系。

### 2.2 关键组件

| 层级 | 路径 | 职责 |
|------|------|------|
| 注解 | `ruoyi-common/.../annotation/Log.java` | 声明模块标题、业务类型、是否存请求/响应、排除字段 |
| 业务类型 | `ruoyi-common/.../enums/BusinessType.java` | OTHER / INSERT / UPDATE / DELETE / GRANT / EXPORT / IMPORT / FORCE / GENCODE / CLEAN |
| 切面 | `ruoyi-framework/.../aspectj/LogAspect.java` | 采集上下文、脱敏、截断、异步落库 |
| 实体 | `ruoyi-system/.../domain/SysOperLog.java` | 与表字段、Excel 导出映射 |
| 持久化 | `SysOperLogMapper.xml` | insert / selectList / delete / clean |
| 管理 API | `SysOperlogController` | 列表、导出、批量删除、清空 |
| 前端 | `frontend/src/views/monitor/operlog/` | 筛选、详情弹窗、导出、清空 |

### 2.3 数据模型（`sys_oper_log`）

| 字段 | 含义 |
|------|------|
| `title` | 模块标题（来自 `@Log.title`） |
| `business_type` | 业务类型枚举序号 |
| `method` | 类名.方法名() |
| `request_method` | HTTP 方法 |
| `operator_type` | 操作人类别（后台/手机/其它） |
| `oper_name` / `dept_name` | 操作人与部门 |
| `oper_url` / `oper_ip` / `oper_location` | 请求路径、IP、归属地 |
| `oper_param` / `json_result` | 请求参数 / 响应（默认最多 2000 字符） |
| `status` / `error_msg` | 成功/失败；失败仅在抛异常时写入 |
| `oper_time` / `cost_time` | 时间与耗时 |

索引：`business_type`、`status`、`oper_time`。

### 2.4 已有能力

- 分页列表 + 多条件筛选（模块、操作人、类型、状态、IP、时间）
- 详情（请求/响应 JSON 格式化、复制）
- 导出 Excel、批量删除、一键清空
- 敏感字段默认排除：`password` / `oldPassword` / `newPassword` / `confirmPassword`
- 代码生成模板对 export/add/edit/delete 会自动带 `@Log`

### 2.5 当前覆盖情况

**已覆盖（RuoYi 原生管理端写操作）**

用户、角色、部门、菜单、岗位、字典、参数、通知公告、个人中心改密、在线用户强退、登录/操作日志管理自身、定时任务、代码生成等——写操作（增删改/导入导出/授权/清空）普遍有 `@Log`；列表与详情 GET 通常不记。

**未覆盖（本仓库业务）**

`ruoyi-blog`、`ruoyi-wechat` 全部 Controller **没有任何 `@Log`**，例如：

- 文章新增/删除、回收站恢复/彻底删除
- 评论审核/删除、举报处理
- 敏感词 CRUD、文件上传删除
- AI Provider / 模块配置保存删除、连通性测试
- AI 智写、优化、流式对话触发
- 账单新增/删除/识图
- 微信账号、菜单、素材、草稿、发布、群发、标签、粉丝同步等

### 2.6 已知局限（设计时必须正视）

| 局限 | 影响 |
|------|------|
| 完全依赖手工 `@Log` | 新接口易漏记；blog/wechat 已全部漏记 |
| 业务失败返回 `AjaxResult.error()` 但不抛异常 | 仍记为「成功」 |
| 参数/结果截断 2000 字符 | 长文、大 JSON、流式响应不完整 |
| 无变更前后快照 / diff | 只能看到请求体，不能回答「改了什么字段」 |
| 无保留策略 | 只能人工清空；全量覆盖后表会快速膨胀 |
| 公开接口 `@Anonymous` | 评论、埋点、微信回调等高频、无登录态，不适合默认记操作日志 |
| AI SSE 流式接口 | 响应体不适合按 `json_result` 落库 |

---

## 3. 目标与非目标

### 3.1 目标

- **可审计**：后台管理端所有「改变系统状态」的操作可追溯（谁、何时、对什么、结果如何）。
- **可落地**：与现有 `@Log` + `sys_oper_log` + 监控页兼容，不引入第二套审计体系（除非明确需要）。
- **可控噪音**：默认不记录纯查询与高频公开流量；必要时可配置扩展。
- **安全**：API Key、密钥、token、密码类字段默认脱敏。

### 3.2 成功指标（上线后度量）

| 目标 | 指标 | 目标值 |
|------|------|--------|
| 管理端写操作可审计 | 业务写接口带 `@Log` 的覆盖率 | 100%（见第 5 节清单） |
| 漏记风险可控 | 新增 Controller 写方法无 `@Log` 的 CI 门禁告警 | 合入前为 0 |
| 查询可用 | 操作日志列表 P95 查询耗时 | 不明显劣化（依赖保留策略） |
| 敏感不外泄 | 日志中出现明文 apiKey/password | 0 |

### 3.3 非目标（本期不做）

- 不把操作日志做成完整「数据变更审计 / 字段级 before-after」
- 不记录所有 GET 列表/详情（除非单独开「查询审计」开关）
- 不把公开前台读写（评论、点赞、埋点、微信服务器回调）默认写入 `sys_oper_log`
- 不改造登录日志体系（继续用 `sys_logininfor`）
- 不做跨服务分布式链路追踪（OpenTelemetry 等）——可列为 Later

---

## 4. 「所有操作」的定义（关键产品决策）

建议将「操作」定义为：

> **已登录用户**在管理端发起的、会 **改变业务或系统状态** 的 HTTP 请求（含导出、同步、测试连通性、AI 生成触发、发布推送等「有副作用」动作）。

按此定义分层：

| 层级 | 是否记操作日志 | 说明 |
|------|----------------|------|
| L1 管理端写操作（POST/PUT/DELETE 及带副作用的 POST） | **必须** | 核心审计范围 |
| L2 管理端敏感读（如导出、下载密钥相关配置） | **必须** | 导出已有；涉及密钥的读取建议记，响应脱敏 |
| L3 管理端普通 GET 列表/详情 | **默认不记** | 噪音大；可选 Feature Flag 开启「查询审计」 |
| L4 匿名公开接口 / 微信回调 / 埋点 | **默认不记** | 走业务表或访问日志，不进操作日志 |
| L5 登录/登出/注册 | **继续走登录日志** | 不混入操作日志 |

若干系人坚持「字面意义的所有 HTTP」，则必须先回答：保留天数、存储容量、列表筛选体验、公开接口隐私合规——否则不建议开工。

---

## 5. 方案比选

### 方案 A：手工补全 `@Log`（延续现有模式）

在 blog/wechat 等 Controller 写方法上逐个加 `@Log(title=..., businessType=...)`。

| 优点 | 缺点 |
|------|------|
| 与 RuoYi 一致，零架构改动 | 易漏；依赖人工纪律 |
| 标题、类型语义清晰 | 每个新接口都要记得加 |
| 可精细控制是否存请求/响应 | 覆盖靠 Code Review |

**工作量**：触碰 blog/wechat 多个 Controller；改动面大但机械。

### 方案 B：全局 AOP 自动拦截写方法 + `@Log` / `@NoLog` 覆盖

默认对匹配 `execution(* com.ruoyi..controller..*(..))` 且 HTTP 方法为 POST/PUT/DELETE（或非 GET）的方法自动记日志；若已有 `@Log` 则用注解元数据；若有 `@NoLog` 则跳过。

| 优点 | 缺点 |
|------|------|
| 新接口默认不漏 | 标题默认只能靠类名/Swagger，语义弱于手工 title |
| 补漏成本低 | 需排除公开、回调、SSE、健康检查 |
| 仍兼容现有 `@Log` | 误伤风险需白名单/黑名单调优 |

### 方案 C：Servlet Filter / Interceptor 记访问日志（另表）

记录所有请求到 `sys_access_log`，与操作日志分离。

| 优点 | 缺点 |
|------|------|
| 真正「全量」 | 与审计语义不同；量大；重复建设 |
| 适合安全溯源 | 管理端「操作日志」产品形态需重做 |

### 推荐

**短期（Now）：方案 A 补齐业务写操作** —— 立刻消除「业务操作不可见」的最大缺口。  
**中期（Next）：方案 B 作为防漏底座** —— 在 A 稳定后引入自动拦截 + `@NoLog`，并用简单静态检查保证写方法要么有 `@Log` 要么有 `@NoLog`。  
**明确不做（或 Later）：方案 C 全量访问日志** —— 除非有合规/安全专项需求。

取舍说明：先把「该记的没记」修好，比先做「不该记的也记」更有价值。

---

## 6. 推荐详细设计

### 6.1 Phase 1 — 业务写操作补全 `@Log`（Now）

#### 6.1.1 标注规范

```java
@Log(title = "博客文章", businessType = BusinessType.INSERT)
@PostMapping
public AjaxResult add(@RequestBody BlogArticle article) { ... }
```

约定：

- `title`：业务模块中文名（与菜单/Swagger 一致），如「博客文章」「AI模型配置」「微信账号」
- `businessType`：INSERT / UPDATE / DELETE / EXPORT / OTHER（同步、测试、推送、AI 生成等用 OTHER，或后续扩展枚举）
- 含密钥字段的接口：`excludeParamNames = {"apiKey", "secret", "token", ...}`，且 `isSaveResponseData = false` 视情况关闭
- AI 流式接口：只记请求元数据，`isSaveResponseData = false`
- 上传接口：文件对象已被切面过滤，仍可记其余参数

#### 6.1.2 建议扩展 `BusinessType`（可选，小改动）

若 OTHER 过多影响筛选，可新增：

- `SYNC` — 同步（粉丝/菜单/素材）
- `TEST` — 连通性测试
- `PUBLISH` — 发布/推送
- `AI` — AI 生成类触发

**取舍**：扩展枚举需同步字典 `sys_oper_type` 与前端筛选选项；若不做扩展，统一用 OTHER + 清晰 `title` 也可接受。

#### 6.1.3 覆盖清单（Phase 1 必须）

**博客管理端写操作**

| Controller | 方法类型 | title 建议 | businessType |
|------------|----------|------------|--------------|
| `BlogArticleController` | 新增/删除 | 博客文章 | INSERT / DELETE |
| `BlogArticleRecycleController` | 恢复/彻底删除 | 文章回收站 | UPDATE / DELETE |
| `BlogCommentController` | 审核/删除 | 博客评论 | UPDATE / DELETE |
| `BlogCommentReportController` | 处理举报 | 评论举报 | UPDATE |
| `BlogSensitiveWordController` | CRUD | 敏感词 | INSERT/UPDATE/DELETE |
| `BlogFileController` | 上传/删除 | 博客文件 | INSERT / DELETE |
| `FileUploadController` | 图片上传 | 文件上传 | INSERT |
| `BlogBillController` | 新增/删除/识图 | 账单 | INSERT/DELETE/OTHER |
| `AiProviderController` | 配置保存/删除/测试/模块配置 | AI模型配置 | INSERT/UPDATE/DELETE/OTHER |
| `AiWriteController` / `AiOptimizeController` | 生成类 | AI智写 / AI优化 | OTHER；不存完整长文本响应或截断即可 |
| `BlogNotificationAdminController` | 发送通知 | 通知管理 | OTHER |
| `BlogNotificationController` | 已读/偏好 | 用户通知 | UPDATE（若视为用户自操作可记） |

**微信管理端写操作**

| Controller | 方法类型 | title 建议 |
|------------|----------|------------|
| `WechatAccountController` | 保存/删除/测试 | 微信账号 |
| `WechatConfigController` | 保存 | 微信配置 |
| `WechatMenuController` | 保存/发布/同步/删除 | 微信菜单 |
| `WechatMaterialController` | 上传/删除 | 微信素材 |
| `WechatDraftController` | 增改删 | 微信草稿 |
| `WechatPublishController` | 推送/提交/删除等 | 微信发布 |
| `WechatMassMessageController` | 预览/群发 | 微信群发 |
| `WechatTagController` | 增删/同步/打标 | 微信标签 |
| `WechatFansController` | 同步 | 微信粉丝 |
| `WechatReplyController` | 保存/删除 | 自动回复 |
| `WechatQrcodeController` | 保存/删除 | 带参二维码 |
| `WechatTemplateMessageController` | 发送 | 模板消息 |
| `WechatKefuController` | 发送 | 客服消息 |

**明确不记（Phase 1）**

- `Public*`：公开文章、评论、埋点
- `PublicWechatController`：微信服务器验签/回调
- 纯 GET 列表/详情/options/dashboard
- `AiController` SSE 流式聊天：若需审计，仅记「发起会话」元数据且关闭响应落库；完整对话内容不进操作日志

#### 6.1.4 脱敏增强（建议与 Phase 1 同做）

在 `LogAspect.EXCLUDE_PROPERTIES` 增加常见敏感名：

`apiKey`、`api_key`、`secret`、`appSecret`、`accessToken`、`token`、`password`

并对 AI Provider / 微信账号保存接口显式 `excludeParamNames`。

#### 6.1.5 失败状态语义（建议小改进）

可选增强：当返回值为 `AjaxResult` 且 `code != 200` 时，将 `status` 记为失败并写入 `msg`。  
**取舍**：改变现有「只有异常才算失败」的行为，需确认是否影响历史解读；建议作为独立小项，不与批量补 `@Log` 绑死。

### 6.2 Phase 2 — 防漏机制（Next）

1. 增加 `@NoLog` 注解，用于明确排除的写方法（若有）。
2. 增加可选自动切面：对未标注 `@Log`/`@NoLog` 的管理端写方法，用默认 title（如类简单名）+ `BusinessType.OTHER` 记一条兜底日志。
3. 仓库脚本或 CI：扫描 `*Controller.java` 中 `PostMapping|PutMapping|DeleteMapping`，要求存在 `@Log` 或 `@NoLog` 或位于公开包/白名单。

### 6.3 Phase 3 — 运维与容量（Next / Later）

全量写操作覆盖后，必须配套：

| 能力 | 建议 |
|------|------|
| 保留策略 | 默认保留 90 天；定时任务清理或按配置清理 |
| 分区/归档 | 数据量大时按月归档（Later） |
| 地址解析 | 继续默认关闭外网 IP 归属地查询；生产按需开启 |
| 查询体验 | 保持现有筛选；必要时增加 `title` 精确字典 |

不做全量 GET 审计前，不强制引入访问日志表。

---

## 7. 技术影响面

### 依赖

- 现有 `LogAspect` / `AsyncFactory` / `sys_oper_log` —— 无破坏性变更即可完成 Phase 1
- 字典 `sys_oper_type` —— 仅在扩展 `BusinessType` 时需同步
- 前端操作日志页 —— Phase 1 无需改；扩展类型时补选项

### 风险

| 风险 | 可能性 | 影响 | 缓解 |
|------|--------|------|------|
| 日志量上升导致磁盘与查询变慢 | 中 | 中 | 保留策略 + 不做公开/GET 全记 |
| API Key 进入 `oper_param` | 中 | 高 | 扩展排除字段 + 接口级 exclude |
| AI/长文参数截断误解为「没记全」 | 高 | 低 | 文档说明 2000 上限；关键操作靠 title+业务主键 |
| 自动切面误记公开接口 | 中（Phase 2） | 中 | 包路径/注解白名单 + `@NoLog` |
| AjaxResult 业务失败仍显示成功 | 已存在 | 中 | Phase 1.5 单独修 |

### 开放问题（开发前建议确认）

- [ ] 「所有操作」是否接受本文 L1+L2 定义，还是要求包含全部 GET？
- [ ] AI 智写/优化是否记录完整 prompt/生成内容，还是只记场景与任务 ID？
- [ ] 是否扩展 `BusinessType` 枚举，还是统一 OTHER？
- [ ] 操作日志保留天数：30 / 90 / 180？
- [ ] Phase 1 是否同步做 AjaxResult 失败判定与脱敏增强？

---

## 8. 发布计划建议

| 阶段 | 内容 | 通过标准 |
|------|------|----------|
| Phase 1a | blog/wechat 管理端写操作补 `@Log` + 脱敏 | 抽样：发文、改 AI 配置、删微信账号均可在操作日志查到；参数无明文密钥 |
| Phase 1b | （可选）AjaxResult 失败状态修正 | 业务失败接口 status=失败 |
| Phase 2 | `@NoLog` + CI 扫描 / 可选自动兜底切面 | 新增写接口合入时不能漏注解 |
| Phase 3 | 定时清理保留策略 | 超期日志被清理；监控页仍可用 |

回滚：去掉新增注解或关闭自动切面配置即可；历史日志表数据可保留。

---

## 9. 结论与建议

**现状**：操作日志是成熟的「注解 → AOP → 异步入库 → 监控页」设计，适合管理端审计；但覆盖完全依赖人工，`ruoyi-blog` / `ruoyi-wechat` 当前是空白。

**建议**：**不要字面全量记录所有 HTTP**。以「管理端有副作用操作 100% 可审计」为目标，先做 Phase 1 补全 `@Log`，再做 Phase 2 防漏与 Phase 3 保留策略。

**下一步（待确认后开工）**：确认第 7 节开放问题 → 按第 6.1.3 清单逐 Controller 补注解 → Maven 编译 + 管理端冒烟验证操作日志可见。

---

## 10. 附录：关键源码索引

- `backend/ruoyi-common/src/main/java/com/ruoyi/common/annotation/Log.java`
- `backend/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/LogAspect.java`
- `backend/ruoyi-framework/src/main/java/com/ruoyi/framework/manager/factory/AsyncFactory.java`
- `backend/ruoyi-system/src/main/java/com/ruoyi/system/domain/SysOperLog.java`
- `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/monitor/SysOperlogController.java`
- `frontend/src/views/monitor/operlog/index.vue`
- `sql/ry_base.sql`（`sys_oper_log` / `sys_logininfor` / `sys_oper_type`）
