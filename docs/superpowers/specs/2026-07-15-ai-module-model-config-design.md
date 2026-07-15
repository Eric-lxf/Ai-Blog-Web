# AI 模块模型配置设计

日期：2026-07-15  
状态：已确认  
分支：`cursor/ai-module-model-config-1d8b`

## 1. 问题

当前所有 AI 调用都由 `DeepSeekServiceImpl` 调用 `AiProviderService.resolveActiveProvider()`，因此编辑器对话、博客智写、评论审核和账单识图共用同一个全局 Provider。

这使不同的任务无法按质量、成本、视觉能力和延迟需求独立选型。当前 `ai_prompt_template.model_name` 虽可按场景记录模型名称，但不能指定 Provider，且账单图片识别不读取模板。`DeepSeekProperties` 还会将 `application.yml` 中绑定的 `DEEPSEEK_API_KEY` 作为数据库配置不可用时的隐式回退。

## 2. 目标与范围

### 目标

- 为六个确定的 AI 功能模块独立配置 Provider。
- 允许模块选择性覆盖文本模型、视觉模型与温度。
- 未配置模块覆盖时，保持使用数据库全局默认 Provider 的兼容行为。
- 移除 YAML 和环境变量 `DEEPSEEK_API_KEY` 的运行时回退；AI 能力只使用 `ai_provider` 中启用的数据库 Provider。
- 将账单建议从通用 `CHAT` 场景隔离为 `BILL_ADVICE`。

### 本期范围

- `editor`：编辑器侧栏聊天、划词改写、扩写、缩写、续写及 `/` 指令。
- `write`：标题、摘要、大纲、全文生成。
- `optimize`：文章优化。
- `comment_moderate`：评论 AI 审核。
- `bill_vision`：账单图片识别。
- `bill_advice`：账单 AI 理财建议。
- 模块配置的后端 API、后台管理界面、运行时解析与迁移脚本。

### 不做

- 提示词模板后台 CRUD。
- 最终用户按请求选择模型。
- 从 Provider API 自动查询或校验模型列表。
- Provider 自动故障转移链路。
- 将历史 `ai_prompt_template.model_name` 字段删除或做数据清洗。

## 3. 方案与取舍

采用独立的 `ai_module_config` 表保存模块覆盖配置。

备选方案是将模块配置放在 `sys_config` 的 JSON 值中，或给 `ai_prompt_template` 增加 Provider 字段。前者没有外键完整性且难以校验；后者将模块与场景耦合，不能覆盖没有模板的视觉识别，并会使编辑器的多个场景产生重复配置。独立表将运行时模型选择与提示词模板职责分离，代价是一张表及其 API、页面实现。

## 4. 数据模型

新增 `ai_module_config`：

| 字段 | 约束 | 含义 |
|---|---|---|
| `id` | 主键 | 配置标识 |
| `module_code` | 非空、唯一 | `editor`、`write`、`optimize`、`comment_moderate`、`bill_vision` 或 `bill_advice` |
| `provider_id` | 非空、索引、逻辑关联 `ai_provider.id` | 模块使用的 Provider |
| `text_model` | 可空 | 文本模型覆盖；为空使用 Provider 的 `default_model` |
| `vision_model` | 可空 | 视觉模型覆盖；为空使用 Provider 的 `vision_model`，再回落 `default_model` |
| `temperature` | 可空 | 温度覆盖；为空使用场景模板温度 |
| `remark` | 可空 | 配置说明 |
| 审计字段 | 与项目现有表一致 | 创建人、创建时间、更新人、更新时间 |

表中不存在模块记录代表该模块继承全局数据库 Provider。`provider_id` 是模块配置记录的必填项；用户通过删除该记录恢复继承，而不是保存空 Provider。

不预置六条模块记录，避免部署后意外改变现有行为。

## 5. 解析规则

新增仅在运行时内部使用的 `AiResolvedModelConfig`，至少包含：

- 实际 `AiProvider`
- 实际文本模型
- 实际视觉模型
- 可选温度覆盖
- 配置来源：模块覆盖、全局默认或首个启用 Provider

解析顺序：

1. 按模块编码查找 `ai_module_config`。
2. 配置存在且其 Provider 已启用、含 API Key 时，使用模块 Provider 及可选模型、温度覆盖。
3. 没有有效模块配置时，读取 `ai.defaultProviderId` 指向的已启用数据库 Provider。
4. 全局默认无效或为空时，使用 ID 最小的启用数据库 Provider。
5. 没有可用数据库 Provider 时，返回明确的“未配置可用 AI Provider”错误。

运行时不再调用 `DeepSeekProperties.isConfigured()`、`fallbackFromYaml()` 或读取 `DEEPSEEK_API_KEY` 作为回退。

`ai_prompt_template` 在本期只负责 system prompt 与默认 temperature。`model_name` 字段保留在数据库和领域对象中以兼容历史数据，但不再参与实际模型解析。

## 6. 模块与场景映射

| 模块 | 场景或能力 |
|---|---|
| `editor` | `CHAT`、`REWRITE`、`EXPAND`、`SHORTEN`、`CONTINUE`、`MERMAID_GEN`、`CODE_GEN` |
| `write` | `TITLE_GEN`、`SUMMARY`、`OUTLINE_GEN`、`FULL_ARTICLE` |
| `optimize` | `REWRITE` |
| `comment_moderate` | `COMMENT_MODERATE` |
| `bill_vision` | `DeepSeekService.recognizeImage` |
| `bill_advice` | 新增 `BILL_ADVICE` 模板 |

`REWRITE` 可以由 `editor` 与 `optimize` 复用同一提示词模板，但其 Provider、模型和温度由调用模块各自解析。账单建议的业务服务必须改为请求 `BILL_ADVICE`，不得再使用 `CHAT`。

## 7. 后端设计

### 配置 API

在现有 `/blog/ai/provider` 管理域新增：

| 方法与路径 | 行为 |
|---|---|
| `GET /blog/ai/provider/module-configs` | 返回六个模块的当前覆盖配置、继承状态及 Provider 选项 |
| `PUT /blog/ai/provider/module-configs/{moduleCode}` | 校验模块编码、启用 Provider、模型字段与温度后保存模块配置 |
| `DELETE /blog/ai/provider/module-configs/{moduleCode}` | 删除模块覆盖，恢复全局 Provider 继承 |

保存接口必须拒绝不存在、未启用或没有 API Key 的 Provider。温度未提供时保存为 `null`，表示继承模板。

删除 Provider 前，服务必须查询 `ai_module_config`。存在引用时拒绝删除，并返回关联的模块编码；不得通过静默清空配置或意外全局回退绕过该约束。

### 调用改造

`DeepSeekService` 的文本补全、流式补全和图片识别入口需要接收显式模块编码。它在加载提示词模板前解析 `AiResolvedModelConfig`，将实际 Provider、模型和温度传给 `LlmClient`。

业务调用方的模块编码固定如下：

- 编辑器控制器和编辑器相关流式调用：`editor`
- `AiWriteServiceImpl`：`write`
- `AiOptimizeServiceImpl`：`optimize`
- `AiCommentModerationServiceImpl`：`comment_moderate`
- `BlogBillServiceImpl.recognize`：`bill_vision`
- `BlogBillServiceImpl.generateAdvice`：`bill_advice`

`AiProviderServiceImpl` 移除 YAML Provider 构造与回退逻辑。`DeepSeekProperties`、相关 Spring 配置注入、`application.yml` / `application-docker.yml` 的 `deepseek` API Key 配置、`.env.example` 和 Docker Compose 的 `DEEPSEEK_API_KEY` 传递一并删除。

## 8. 前端设计

在既有“AI模型配置”页面新增“按功能模块配置”区块，不新建菜单或路由。

每个模块展示：

- 用户可理解的模块名称和用途。
- 当前状态：继承全局或已覆盖。
- Provider 下拉框。
- 文本模型输入框。
- 视觉模型输入框。
- 温度输入框。
- 备注输入框。
- 保存按钮和“恢复继承”操作。

视觉模型仅对 `bill_vision` 显示；其他模块仅显示文本模型。温度对文本模块显示；`bill_vision` 不显示温度。接口返回的 Provider 选项必须包含启用状态，前端只允许选择启用项。

全局默认 Provider 的现有 UI 保留，并明确标注为“未配置模块覆盖时的数据库回退”。

## 9. 迁移与兼容

1. 新增 `ai_module_config` 表与索引。
2. 新增 `BILL_ADVICE` 的 `ai_prompt_template` 种子记录；其提示词承接当前账单建议的用途。
3. 不写入模块覆盖记录，因此现有运行时请求仍使用 `ai.defaultProviderId` 或首个启用数据库 Provider。
4. 删除环境变量和 YAML 回退代码与部署配置。
5. 未配置任何启用数据库 Provider 的部署会从“环境变量可继续运行”变为明确配置错误；这是有意的行为变更，部署前必须在后台创建并启用至少一个 Provider。

## 10. 错误处理

| 情形 | 行为 |
|---|---|
| 模块编码不在六个允许值内 | 配置 API 返回参数错误 |
| 模块配置引用禁用或无 Key 的 Provider | 保存拒绝；历史配置在运行时跳过并回退全局 Provider |
| 模块 Provider 不可用且全局 Provider 可用 | 使用全局 Provider |
| 没有任何可用数据库 Provider | 文本、流式和视觉接口返回统一配置错误 |
| 删除仍被模块引用的 Provider | 删除拒绝并返回引用模块 |
| 缺少 `BILL_ADVICE` 模板 | 账单建议接口返回“未找到可用的 AI 提示词模板” |

## 11. 验收与验证

后端验证：

- 六个模块可以独立保存 Provider 和可选模型、温度覆盖。
- 文本模型、视觉模型和温度为空时按设计规则继承。
- 删除模块配置后，该模块恢复全局数据库 Provider。
- 全局默认无效时使用首个启用数据库 Provider。
- 删除被模块引用的 Provider 被拒绝。
- 清空或禁用所有数据库 Provider 后，AI 请求失败，且不读取 YAML 或环境变量。
- 账单建议使用 `BILL_ADVICE`，不使用 `CHAT`。

构建与手工验证：

- 在 `backend/` 执行 `mvn -B -DskipTests package -pl ruoyi-admin -am`。
- 在 `frontend/` 执行 `npm run build:prod`。
- 管理后台分别保存一个文本模块和 `bill_vision` 模块配置，调用对应能力确认配置生效。
- 删除两个模块的覆盖，确认其恢复全局数据库 Provider。

## 12. 相关实现位置

- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/DeepSeekServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiConfigServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/llm/LlmClientImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiWriteServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiOptimizeServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiCommentModerationServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/BlogBillServiceImpl.java`
- `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/config/DeepSeekProperties.java`
- `frontend/src/views/blog/ai/provider/index.vue`
- `frontend/src/api/blog/aiProvider.js`
- `sql/ai_provider_schema.sql`
- `sql/blog_schema.sql`
- `sql/blog_comment_schema.sql`
