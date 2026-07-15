# AI 模块模型配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让六个固定 AI 功能模块独立选择数据库 Provider，并可覆盖文本模型、视觉模型与温度，同时移除 YAML / `DEEPSEEK_API_KEY` 回退。

**Architecture:** 新建 `ai_module_config` 保存模块覆盖；`AiProviderService` 统一解析“模块覆盖 → 数据库全局默认 → 首个启用 Provider”。`DeepSeekService` 接收模块编码并把已解析的 Provider、模型与温度传给 `LlmClient`，业务服务不自行选择模型。后台在既有 AI 模型配置页面维护六个模块覆盖，删除配置即恢复继承。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis-Plus、OkHttp、Vue 3、Element Plus、MySQL 8、Maven、npm。

## Global Constraints

- 模块编码只能是 `editor`、`write`、`optimize`、`comment_moderate`、`bill_vision`、`bill_advice`。
- `ai_module_config.provider_id` 必填；没有记录表示继承，不保存空 Provider。
- 运行时回退只能使用数据库 `ai.defaultProviderId` 或首个启用 Provider；不得读取 YAML 或 `DEEPSEEK_API_KEY`。
- `ai_prompt_template` 本期只提供 system prompt 与默认温度；其 `model_name` 不参与模型解析。
- 不新增菜单、路由、提示词模板 CRUD、最终用户模型选择、模型自动发现或 Provider 自动故障转移。
- 仅修改实现本设计所必需的文件，不做无关重构。

---

## 文件结构

| 路径 | 责任 |
|---|---|
| `sql/ai_module_config_schema.sql` | 模块配置表定义 |
| `domain/AiModuleConfig.java` | 模块配置持久化实体 |
| `mapper/AiModuleConfigMapper.java` | MyBatis-Plus Mapper |
| `constant/AiModuleCode.java` | 六个允许的模块编码 |
| `service/AiResolvedModelConfig.java` | 运行时解析结果 |
| `dto/AiModuleOverrideSaveRequest.java` | 保存模块覆盖请求 |
| `vo/AiFeatureModuleConfigItemVO.java` | 单模块配置响应 |
| `vo/AiFeatureModuleConfigsVO.java` | 六模块配置响应 |
| `AiConfigService(Impl)` | 模块配置 CRUD |
| `AiProviderService(Impl)` | Provider 解析与删除引用保护 |
| `DeepSeekService(Impl)` / `LlmClient(Impl)` | 将模块解析结果应用到模型调用 |
| `AiProviderController` | 模块配置 REST API |
| `frontend/src/api/blog/aiProvider.js` | 模块配置 API 客户端 |
| `frontend/src/views/blog/ai/provider/index.vue` | 模块配置表单 |

## Task 1: 建立表、场景种子与领域类型

**Files:**
- Create: `sql/ai_module_config_schema.sql`
- Modify: `sql/blog_schema.sql`
- Modify: `sql/ai_provider_schema.sql`
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/domain/AiModuleConfig.java`
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/mapper/AiModuleConfigMapper.java`
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/constant/AiModuleCode.java`
- Modify: `README.md`
- Modify: `docker-compose.yml`

**Interfaces:**
- Produces `AiModuleConfig`，字段为 `id`、`moduleCode`、`providerId`、`textModel`、`visionModel`、`temperature`、`remark`、`createTime`、`updateTime`。
- Produces `AiModuleCode.isSupported(String)` 与 `AiModuleCode.all()`。

- [ ] **Step 1: 添加允许模块编码测试**

Create `backend/ruoyi-blog/src/test/java/com/ruoyi/blog/constant/AiModuleCodeTest.java`:

```java
package com.ruoyi.blog.constant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiModuleCodeTest
{
    @Test
    void acceptsOnlyTheSixFeatureModules()
    {
        assertTrue(AiModuleCode.isSupported(AiModuleCode.EDITOR));
        assertTrue(AiModuleCode.isSupported(AiModuleCode.BILL_ADVICE));
        assertFalse(AiModuleCode.isSupported("chat"));
        assertFalse(AiModuleCode.isSupported(null));
    }
}
```

- [ ] **Step 2: 加入测试依赖并确认测试先失败**

Add to `backend/ruoyi-blog/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiModuleCodeTest test`  
Expected: FAIL，因为 `AiModuleCode` 尚不存在。

- [ ] **Step 3: 创建模块常量、实体、Mapper 与迁移**

Create `AiModuleCode.java`:

```java
package com.ruoyi.blog.constant;

import java.util.List;

public final class AiModuleCode
{
    public static final String EDITOR = "editor";
    public static final String WRITE = "write";
    public static final String OPTIMIZE = "optimize";
    public static final String COMMENT_MODERATE = "comment_moderate";
    public static final String BILL_VISION = "bill_vision";
    public static final String BILL_ADVICE = "bill_advice";

    private static final List<String> ALL = List.of(EDITOR, WRITE, OPTIMIZE, COMMENT_MODERATE, BILL_VISION, BILL_ADVICE);

    private AiModuleCode() {}

    public static boolean isSupported(String moduleCode)
    {
        return ALL.contains(moduleCode);
    }

    public static List<String> all()
    {
        return ALL;
    }
}
```

Create `sql/ai_module_config_schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS `ai_module_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_code` varchar(32) NOT NULL COMMENT 'editor/write/optimize/comment_moderate/bill_vision/bill_advice',
  `provider_id` bigint NOT NULL COMMENT 'ai_provider.id',
  `text_model` varchar(128) DEFAULT NULL COMMENT '文本模型覆盖，空则 provider.default_model',
  `vision_model` varchar(128) DEFAULT NULL COMMENT '视觉模型覆盖，空则 provider.vision_model/default_model',
  `temperature` decimal(3,2) DEFAULT NULL COMMENT '温度覆盖，空则提示词模板温度',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_module_config_code` (`module_code`),
  KEY `idx_ai_module_config_provider` (`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 功能模块模型配置';
```

Create the entity and Mapper following `AiProvider.java` / `AiProviderMapper.java`; the entity uses `@TableName("ai_module_config")`, Lombok `@Data`, and matching Java types (`Long`, `String`, `BigDecimal`, `Date`).

Append this idempotent `BILL_ADVICE` seed to `sql/blog_schema.sql`:

```sql
INSERT INTO `ai_prompt_template` (`template_name`, `scene_type`, `system_prompt`, `model_name`, `temperature`, `is_active`)
SELECT '账单理财建议', 'BILL_ADVICE',
       '你是专业且审慎的理财助手。基于用户提供的账单统计生成建议，只返回 JSON 数组，每项包含 tone、title、detail；不得编造账单中不存在的数据。',
       'deepseek-chat', 0.70, 1
WHERE NOT EXISTS (SELECT 1 FROM `ai_prompt_template` WHERE `scene_type` = 'BILL_ADVICE' LIMIT 1);
```

Remove the `DEEPSEEK_API_KEY` wording from the `ai.defaultProviderId` `sys_config` remark in `sql/ai_provider_schema.sql`. Add `11-ai_module_config_schema.sql` to the database-init mount list in `docker-compose.yml` immediately after `10-ai_provider_schema.sql`, and add the script to the README SQL application order.

- [ ] **Step 4: 运行单元测试**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiModuleCodeTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-blog/pom.xml backend/ruoyi-blog/src/test/java/com/ruoyi/blog/constant/AiModuleCodeTest.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/constant/AiModuleCode.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/domain/AiModuleConfig.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/mapper/AiModuleConfigMapper.java sql/ai_module_config_schema.sql sql/blog_schema.sql sql/ai_provider_schema.sql docker-compose.yml README.md
git commit -m "feat(ai): add module model configuration schema"
```

## Task 2: 实现模块 Provider 解析与数据库回退

**Files:**
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/AiResolvedModelConfig.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/AiProviderService.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java`
- Test: `backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/AiProviderResolverTest.java`

**Interfaces:**
- Produces `AiResolvedModelConfig resolveForModule(String moduleCode)`.
- `AiResolvedModelConfig` exposes `AiProvider provider`、`String textModel`、`String visionModel`、`BigDecimal temperatureOverride` 与 `ConfigSource source`。

- [ ] **Step 1: 编写解析回退测试**

Create an isolated unit test using mocked `AiProviderMapper` and `AiConfigService`:

```java
@Test
void moduleOverrideWinsAndUsesItsModelOverrides()
{
    AiProvider provider = enabledProvider(9L, "module-default", "module-vision");
    AiModuleConfig override = new AiModuleConfig();
    override.setModuleCode(AiModuleCode.WRITE);
    override.setProviderId(9L);
    override.setTextModel("write-model");
    override.setTemperature(new BigDecimal("0.60"));

    when(moduleConfigMapper.selectOne(any())).thenReturn(override);
    when(providerMapper.selectById(9L)).thenReturn(provider);

    AiResolvedModelConfig resolved = service.resolveForModule(AiModuleCode.WRITE);

    assertEquals("write-model", resolved.getTextModel());
    assertEquals("module-vision", resolved.getVisionModel());
    assertEquals(new BigDecimal("0.60"), resolved.getTemperatureOverride());
    assertEquals(ConfigSource.MODULE_OVERRIDE, resolved.getSource());
}
```

Add test cases for an invalid override Provider falling back to `ai.defaultProviderId`, for absent global default selecting the first enabled Provider, and for no Provider throwing `ServiceException` with `未配置可用 AI Provider`.

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiProviderResolverTest test`  
Expected: FAIL，因为解析结果类型和 `resolveForModule` 尚不存在。

- [ ] **Step 3: 添加解析结果与服务实现**

Create an immutable result type:

```java
@Value
public class AiResolvedModelConfig
{
    AiProvider provider;
    String textModel;
    String visionModel;
    BigDecimal temperatureOverride;
    ConfigSource source;

    public enum ConfigSource
    {
        MODULE_OVERRIDE, GLOBAL_DEFAULT, FIRST_ENABLED
    }
}
```

Add to `AiProviderService`:

```java
AiResolvedModelConfig resolveForModule(String moduleCode);
```

In `AiProviderServiceImpl`, inject `AiModuleConfigMapper`; resolve an enabled Provider only when it has text in `apiKey`; compute models exactly as:

```java
String textModel = StringUtils.hasText(config.getTextModel()) ? config.getTextModel() : provider.getDefaultModel();
String visionModel = StringUtils.hasText(config.getVisionModel()) ? config.getVisionModel()
        : StringUtils.hasText(provider.getVisionModel()) ? provider.getVisionModel() : provider.getDefaultModel();
```

For an absent or invalid module override, apply existing database default then first enabled Provider selection. Do not call `fallbackFromYaml()`. Refactor `resolveActiveProvider()` to return the Provider from the same database-only default/first-enabled path so existing status and test APIs remain consistent.

- [ ] **Step 4: 运行解析测试**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiProviderResolverTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/AiResolvedModelConfig.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/AiProviderService.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/AiProviderResolverTest.java
git commit -m "feat(ai): resolve providers by feature module"
```

## Task 3: 将实际模型与温度传递给 LLM 客户端

**Files:**
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/llm/LlmClient.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/llm/LlmClientImpl.java`
- Test: `backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/llm/LlmClientModelResolutionTest.java`

**Interfaces:**
- `chatCompletion` / `streamChat` 接收显式文本模型和有效温度。
- `recognizeImage` 接收显式视觉模型。
- `testConnection` 保持使用 `provider.defaultModel`，不依赖模块配置。

- [ ] **Step 1: 编写模板模型被忽略的测试**

Extract the package-visible text-model helper in `LlmClientImpl` and test:

```java
@Test
void usesResolvedModelInsteadOfLegacyTemplateModel()
{
    AiProvider provider = new AiProvider();
    provider.setDefaultModel("provider-default");
    AiPromptTemplate template = new AiPromptTemplate();
    template.setModelName("legacy-template-model");

    assertEquals("resolved-model", LlmClientImpl.resolveTextModel("resolved-model", provider, template));
}
```

Also assert that a null resolved text model returns `provider.getDefaultModel()` and a null resolved vision model returns `provider.getVisionModel()` before `provider.getDefaultModel()`.

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=LlmClientModelResolutionTest test`  
Expected: FAIL，因为显式模型解析 helper 尚不存在。

- [ ] **Step 3: 修改 LLM 接口与请求构建**

Update the interface to receive resolved values:

```java
String chatCompletion(AiProvider provider, AiCompletionRequest request, AiPromptTemplate template,
        String textModel, BigDecimal effectiveTemperature, OkHttpClient client);

void streamChat(AiProvider provider, AiChatRequest request, AiPromptTemplate template,
        String textModel, BigDecimal effectiveTemperature, OkHttpClient client, SseEmitter emitter) throws Exception;

String recognizeImage(AiProvider provider, String imageUrl, String textPrompt,
        String visionModel, OkHttpClient client);
```

Update both OpenAI-compatible and Anthropic request payload paths in `LlmClientImpl` to use explicit `textModel`; update image payload generation to use explicit `visionModel`. Compute effective temperature in the caller with this strict order:

```java
BigDecimal effectiveTemperature = request.getTemperature() != null
        ? request.getTemperature()
        : resolved.getTemperatureOverride() != null ? resolved.getTemperatureOverride() : template.getTemperature();
```

Do not read `template.getModelName()` in a runtime request path.

- [ ] **Step 4: 运行模型解析测试**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=LlmClientModelResolutionTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/llm/LlmClient.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/llm/LlmClientImpl.java backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/llm/LlmClientModelResolutionTest.java
git commit -m "refactor(ai): pass resolved models to llm client"
```

## Task 4: 将模块上下文接入 AI 门面与业务调用

**Files:**
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/DeepSeekService.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/DeepSeekServiceImpl.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/controller/AiController.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiWriteServiceImpl.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiOptimizeServiceImpl.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiCommentModerationServiceImpl.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/BlogBillServiceImpl.java`
- Test: `backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/BlogBillAdviceSceneTest.java`

**Interfaces:**

```java
void streamChat(AiChatRequest request, SseEmitter emitter, String moduleCode);
String chatCompletion(AiCompletionRequest request, String moduleCode);
String recognizeImage(String imageUrl, String textPrompt, String moduleCode);
```

- [ ] **Step 1: 编写账单建议场景测试**

Use a mocked `DeepSeekService` to assert that `BlogBillServiceImpl` constructs the completion request with `BILL_ADVICE` and passes `AiModuleCode.BILL_ADVICE`:

```java
verify(deepSeekService).chatCompletion(
        argThat(request -> "BILL_ADVICE".equals(request.getScene())),
        eq(AiModuleCode.BILL_ADVICE));
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=BlogBillAdviceSceneTest test`  
Expected: FAIL，因为现有账单建议场景是 `CHAT`，且没有模块参数。

- [ ] **Step 3: 接入解析结果并固定调用模块**

In `DeepSeekServiceImpl`, call `aiProviderService.resolveForModule(moduleCode)` at each entry point, pass `resolved.getProvider()` and its models to `LlmClient`, and use the effective-temperature rule from Task 3. Keep current stream error completion mechanics, but replace the API key message with `未配置可用 AI Provider，请在「AI模型配置」中添加并启用 Provider`.

Set callers as follows:

```java
deepSeekService.streamChat(request, emitter, AiModuleCode.EDITOR);
deepSeekService.chatCompletion(request, AiModuleCode.WRITE);
deepSeekService.chatCompletion(request, AiModuleCode.OPTIMIZE);
deepSeekService.chatCompletion(request, AiModuleCode.COMMENT_MODERATE);
deepSeekService.recognizeImage(imageUrl, RECOGNIZE_PROMPT, AiModuleCode.BILL_VISION);
deepSeekService.chatCompletion(adviceRequest, AiModuleCode.BILL_ADVICE);
```

Build `adviceRequest` with `setScene("BILL_ADVICE")`. For template lookup, keep ordinary scene behavior for existing flows but make `BILL_ADVICE` strictly error when absent; it must not silently use `CHAT`.

- [ ] **Step 4: 运行账单测试与模块编译**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=BlogBillAdviceSceneTest,AiProviderResolverTest,LlmClientModelResolutionTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/DeepSeekService.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/DeepSeekServiceImpl.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/controller/AiController.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiWriteServiceImpl.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiOptimizeServiceImpl.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiCommentModerationServiceImpl.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/BlogBillServiceImpl.java backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/BlogBillAdviceSceneTest.java
git commit -m "feat(ai): route calls through module configurations"
```

## Task 5: 暴露模块配置 API 并保护 Provider 删除

**Files:**
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/dto/AiModuleOverrideSaveRequest.java`
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/vo/AiFeatureModuleConfigItemVO.java`
- Create: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/vo/AiFeatureModuleConfigsVO.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/AiConfigService.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiConfigServiceImpl.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/controller/AiProviderController.java`
- Test: `backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/AiModuleConfigServiceTest.java`

**Interfaces:**

```java
AiFeatureModuleConfigsVO listFeatureModuleConfigs();
void saveFeatureModuleOverride(String moduleCode, AiModuleOverrideSaveRequest request);
void deleteFeatureModuleOverride(String moduleCode);
```

- [ ] **Step 1: 编写服务验证测试**

Cover these cases:

```java
assertThrows(ServiceException.class, () ->
        service.saveFeatureModuleOverride(AiModuleCode.WRITE, requestWithProviderId(99L)));

service.deleteFeatureModuleOverride(AiModuleCode.WRITE);
verify(moduleConfigMapper).delete(any());

assertThrows(ServiceException.class, () -> providerService.delete(9L));
```

Stub provider `99L` as disabled for the first assertion. Stub a module configuration referencing `9L` for the provider-delete assertion.

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiModuleConfigServiceTest test`  
Expected: FAIL，因为模块配置 CRUD 尚不存在。

- [ ] **Step 3: 实现 DTO、VO、服务与控制器**

Use this request shape:

```java
@Data
public class AiModuleOverrideSaveRequest
{
    @NotNull
    private Long providerId;
    private String textModel;
    private String visionModel;
    @DecimalMin("0.00")
    @DecimalMax("2.00")
    private BigDecimal temperature;
    private String remark;
}
```

`listFeatureModuleConfigs()` must always return six items in `AiModuleCode.all()` order. Each item indicates `inherited=true` when no row exists and includes current override fields only when a row exists. It also returns `aiProviderService.listOptions()` so the frontend can populate selectors.

Add controller routes:

```java
@GetMapping("/module-configs")
public AjaxResult featureModuleConfigs() { return AjaxResult.success(aiConfigService.listFeatureModuleConfigs()); }

@PutMapping("/module-configs/{moduleCode}")
public AjaxResult saveFeatureModuleConfig(@PathVariable String moduleCode,
        @Valid @RequestBody AiModuleOverrideSaveRequest request) { ... }

@DeleteMapping("/module-configs/{moduleCode}")
public AjaxResult deleteFeatureModuleConfig(@PathVariable String moduleCode) { ... }
```

Use `blog:ai:provider:query` or `list` for GET, `edit` for PUT, and `remove` for DELETE. Validate `AiModuleCode.isSupported(moduleCode)` before every save/delete. Before `AiProviderServiceImpl.delete(id)` deletes a row, query `AiModuleConfigMapper` for references and throw `ServiceException("该 Provider 正被模块配置使用：...")` when present.

- [ ] **Step 4: 运行服务测试**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiModuleConfigServiceTest,AiProviderResolverTest test`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/ruoyi-blog/src/main/java/com/ruoyi/blog/dto/AiModuleOverrideSaveRequest.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/vo/AiFeatureModuleConfigItemVO.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/vo/AiFeatureModuleConfigsVO.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/AiConfigService.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiConfigServiceImpl.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/controller/AiProviderController.java backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/AiModuleConfigServiceTest.java
git commit -m "feat(ai): add module configuration management api"
```

## Task 6: 移除 YAML 与环境变量 Provider 回退

**Files:**
- Delete: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/config/DeepSeekProperties.java`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/config/HttpClientConfig.java`
- Modify: `backend/ruoyi-blog/src/main/resources/application.yml`
- Modify: `backend/ruoyi-blog/src/main/resources/application-docker.yml`
- Modify: `backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java`
- Modify: `.env.example`
- Modify: `docker-compose.yml`
- Modify: `frontend/src/components/Editor/EnhancedMarkdownEditor.vue`

**Interfaces:**
- `deepSeekOkHttpClient` becomes a fixed-default-timeout `OkHttpClient` bean; per-Provider timeout remains implemented in `AiProviderServiceImpl.httpClient`.
- No source file, YAML value, Compose environment variable, or UI copy refers to `DEEPSEEK_API_KEY`.

- [ ] **Step 1: 编写无数据库 Provider 时不回退的测试**

Add to `AiProviderResolverTest`:

```java
@Test
void noDatabaseProviderFailsInsteadOfUsingYamlFallback()
{
    when(moduleConfigMapper.selectOne(any())).thenReturn(null);
    when(aiConfigService.getDefaultProviderId()).thenReturn(null);
    when(providerMapper.selectOne(any())).thenReturn(null);

    ServiceException error = assertThrows(ServiceException.class,
            () -> service.resolveForModule(AiModuleCode.EDITOR));

    assertEquals("未配置可用 AI Provider", error.getMessage());
}
```

- [ ] **Step 2: 运行测试确认失败或验证旧回退仍存在**

Run: `cd backend && mvn -pl ruoyi-blog -Dtest=AiProviderResolverTest#noDatabaseProviderFailsInsteadOfUsingYamlFallback test`  
Expected: PASS only after Task 2 resolver no longer references `fallbackFromYaml`; before removal, inspect that no fallback method remains.

- [ ] **Step 3: 删除回退配置**

Delete `DeepSeekProperties.java`. Replace the Http client bean with:

```java
@Bean
public OkHttpClient deepSeekOkHttpClient()
{
    return new OkHttpClient.Builder().readTimeout(300, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
}
```

Remove the `deepseek:` blocks from both application YAML files, remove `DEEPSEEK_API_KEY` from `.env.example` and Compose service environment, and update the editor empty-state copy to direct administrators only to “AI模型配置”. Remove imports, constructor fields, `fallbackFromYaml()`, and all `DeepSeekProperties` references.

- [ ] **Step 4: 确认没有遗留配置引用**

Run: `rg -n "DEEPSEEK_API_KEY|DeepSeekProperties|fallbackFromYaml|deepseek\\.api-key" backend frontend docker-compose.yml .env.example`  
Expected: no matches.

- [ ] **Step 5: Commit**

```bash
git add -u backend/ruoyi-blog/src/main/java/com/ruoyi/blog/config/DeepSeekProperties.java
git add backend/ruoyi-blog/src/main/java/com/ruoyi/blog/config/HttpClientConfig.java backend/ruoyi-blog/src/main/java/com/ruoyi/blog/service/impl/AiProviderServiceImpl.java backend/ruoyi-blog/src/main/resources/application.yml backend/ruoyi-blog/src/main/resources/application-docker.yml .env.example docker-compose.yml frontend/src/components/Editor/EnhancedMarkdownEditor.vue backend/ruoyi-blog/src/test/java/com/ruoyi/blog/service/AiProviderResolverTest.java
git commit -m "refactor(ai): remove yaml provider fallback"
```

## Task 7: 增加模块配置后台界面

**Files:**
- Modify: `frontend/src/api/blog/aiProvider.js`
- Modify: `frontend/src/views/blog/ai/provider/index.vue`

**Interfaces:**

```javascript
export function listFeatureModuleConfigs() {
  return request({ url: '/blog/ai/provider/module-configs', method: 'get' })
}

export function saveFeatureModuleOverride(moduleCode, data) {
  return request({ url: `/blog/ai/provider/module-configs/${moduleCode}`, method: 'put', data })
}

export function deleteFeatureModuleOverride(moduleCode) {
  return request({ url: `/blog/ai/provider/module-configs/${moduleCode}`, method: 'delete' })
}
```

- [ ] **Step 1: 编写前端 API 调用的失败测试或静态接口校验**

If the repository still has no frontend test runner, add no test framework. First verify the new endpoints are absent:

Run: `rg -n "module-configs|listFeatureModuleConfigs" frontend/src/api/blog/aiProvider.js`  
Expected: no matches.

- [ ] **Step 2: 添加 API 客户端与页面状态**

Add the three API functions above while keeping `getAiModuleConfig` and `saveAiModuleConfig` for the global database default. In `provider/index.vue`, load module rows with providers after the existing Provider configuration loads:

```javascript
const moduleConfigs = ref([])
const moduleProviderOptions = ref([])

async function loadFeatureModuleConfigs() {
  const { data } = await listFeatureModuleConfigs()
  moduleConfigs.value = data.items
  moduleProviderOptions.value = data.providerOptions.filter(item => item.enabled === 1)
}
```

- [ ] **Step 3: 增加模块配置区块**

Render one row for each API-supplied module. Show:

```vue
<el-tag :type="row.inherited ? 'info' : 'success'">
  {{ row.inherited ? '继承全局默认' : '已覆盖' }}
</el-tag>
<el-select v-model="row.providerId" :disabled="row.inherited">
  <el-option v-for="provider in moduleProviderOptions" :key="provider.id"
             :label="`${provider.name}（${provider.defaultModel}）`" :value="provider.id" />
</el-select>
<el-button v-if="!row.inherited" link type="danger" @click="restoreInheritance(row)">
  恢复继承
</el-button>
```

Use a local “编辑配置” action to make inherited rows editable; save calls `saveFeatureModuleOverride`. Show text model for every text module, vision model only for `bill_vision`, and hide temperature for `bill_vision`. Label the existing global Provider selector as “全局数据库默认（模块未覆盖时使用）”.

- [ ] **Step 4: 验证 API 引用与生产构建**

Run: `rg -n "listFeatureModuleConfigs|saveFeatureModuleOverride|deleteFeatureModuleOverride" frontend/src/api/blog/aiProvider.js frontend/src/views/blog/ai/provider/index.vue && cd frontend && npm run build:prod`  
Expected: three API functions are referenced and Vite production build exits 0.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/blog/aiProvider.js frontend/src/views/blog/ai/provider/index.vue
git commit -m "feat(ai): configure models by feature module"
```

## Task 8: 全量验证与手工冒烟

**Files:**
- Modify only if validation exposes a defect in a task above.

**Interfaces:**
- No new API or model interface; this task proves the completed behavior.

- [ ] **Step 1: 运行后端单元测试与多模块构建**

Run: `cd backend && mvn -B -pl ruoyi-blog test && mvn -B -DskipTests package -pl ruoyi-admin -am`  
Expected: all new unit tests pass and the deployable admin JAR packages successfully.

- [ ] **Step 2: 运行前端生产构建**

Run: `cd frontend && npm run build:prod`  
Expected: Vite build exits 0.

- [ ] **Step 3: 启动依赖与应用**

Run:

```bash
sudo docker compose --env-file .env up mysql redis -d
cd backend && java -jar ruoyi-admin/target/ruoyi-admin.jar
```

Expected: application starts and `curl -s http://localhost:8080/captchaImage` returns a successful JSON payload.

- [ ] **Step 4: 手工验证配置继承和模块覆盖**

1. 登录后台，进入“AI模型配置”，确保至少有两个已启用 Provider。
2. 配置全局数据库默认 Provider A。
3. 为 `write` 保存 Provider B、文本模型覆盖和温度覆盖。
4. 调用“博客智写”的标题生成；确认后端日志/请求使用 Provider B 和指定模型。
5. 删除 `write` 覆盖后再次生成标题；确认使用 Provider A。
6. 为 `bill_vision` 保存视觉模型覆盖并识别账单图片；确认使用该视觉模型。
7. 删除仍被 `write` 或 `bill_vision` 引用的 Provider；确认后台返回关联模块错误。
8. 禁用或删除所有数据库 Provider 后调用 AI；确认返回“未配置可用 AI Provider”，而非读取环境变量。

- [ ] **Step 5: 最终差异检查与提交**

Run:

```bash
git diff --check
git status --short
```

Expected: no whitespace error and only intended tracked changes.

If validation required code changes:

```bash
git add <changed-files>
git commit -m "fix(ai): address module configuration validation"
```

## Plan Self-Review

| 设计要求 | 覆盖任务 |
|---|---|
| 六个模块独立 Provider/模型/温度 | Task 1、2、4、5、7 |
| 模块 → 全局数据库 → 首个启用解析 | Task 2 |
| 不使用 YAML / 环境变量 | Task 2、6、8 |
| `model_name` 不再影响实际模型 | Task 3 |
| `BILL_ADVICE` 场景隔离 | Task 1、4 |
| Provider 删除保护 | Task 5、8 |
| 无提示词模板后台 | Global Constraints |
| 后端、前端、手工验证 | Task 2–8 |

每个实施任务均包含聚焦提交以及测试或构建检查点。
