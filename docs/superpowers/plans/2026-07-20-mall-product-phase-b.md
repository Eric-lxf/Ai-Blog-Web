# Mall Product Phase B Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `ruoyi-mall-product` 落地属性库、类目属性绑定、前台类目与映射，并让发品/公开列表按设计校验与导购。

**Architecture:** 现有 `mall_category` 升级为后台类目；新增前台类目/映射与属性相关表；SKU 继续用 `specs_json`；公开 `categoryId` 改为前台 id 再展开后台叶子过滤。全部在模块化单体内完成，不新建 Maven 模块。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、Vue3 + Element Plus、MySQL `nova_mall`

**Spec:** `docs/superpowers/specs/2026-07-20-mall-product-phase-b-design.md`

## Global Constraints

- JDK 17+；在 `backend/` 下 `mvn -pl ruoyi-mall-product -am -DskipTests compile`
- POST 用 `@RequestBody`；GET 不用 `@RequestBody`
- 最小改动；权限前缀 `mall:attr:*`、`mall:frontCategory:*`
- 无 SALE 绑定的类目允许自由 `specs_json`
- 服务端不强制笛卡尔积生成 SKU
- 时区 Asia/Shanghai（沿用现有）

---

### Task 1: DDL + 迁移 + 菜单种子

**Files:**
- Create: `sql/mall_attr_front_category_schema.sql`
- Create: `sql/mall_phase_b_migrate_front_category.sql`
- Create: `sql/mall_phase_b_menu_seed.sql`
- Create: `sql/mall_phase_b_attr_demo_seed.sql`（可选演示：颜色/尺码）
- Modify: `README.md`（在 mall SQL 执行顺序中追加上述脚本）

**Interfaces:**
- Produces: 表 `mall_attr`、`mall_attr_option`、`mall_category_attr`、`mall_spu_attr_value`、`mall_front_category`、`mall_front_category_rel`；菜单 id `3010`/`3011` 及按钮 `3150–3163`

- [ ] **Step 1: 写 schema DDL**

`mall_attr`: id, name varchar(64), input_type varchar(16) NOT NULL DEFAULT 'text' COMMENT 'text|select|multi', status char(1) default '0', sort int, create_by/time, update_by/time, remark  
`mall_attr_option`: id, attr_id, value varchar(128), sort, status  
`mall_category_attr`: id, category_id, attr_id, attr_type varchar(8) COMMENT 'SALE|DESC', required char(1) default '0', sort；UNIQUE(category_id, attr_id)  
`mall_spu_attr_value`: id, spu_id, attr_id, value varchar(512)；UNIQUE(spu_id, attr_id)  
`mall_front_category`: 同 `mall_category` 字段结构（parent_id/name/sort/status/icon/audit/remark）  
`mall_front_category_rel`: id, front_id, back_category_id；UNIQUE(front_id, back_category_id)

- [ ] **Step 2: 写迁移脚本**

按 `parent_id` 层级把每个 `mall_category` 插入同名 `mall_front_category`（可用临时映射表 `back_id → front_id`），再插入 1:1 rel。要求可重复执行时用「若 front 已有数据则跳过」或文档注明只跑一次。

- [ ] **Step 3: 菜单种子**

```sql
-- 在 3000 下增加：
-- 3010 属性管理 mall/admin/attr/index  mall:attr:list
-- 3011 前台类目 mall/admin/frontCategory/index  mall:frontCategory:list
-- 按钮权限 3150–3153 attr；3160–3163 frontCategory
-- UPDATE 3001 菜单名称为「后台类目」（可选）
INSERT IGNORE INTO sys_role_menu ...
```

- [ ] **Step 4: 更新 README SQL 顺序**

在 `mall_product_schema.sql` 之后追加 Phase B 四个脚本名。

- [ ] **Step 5: Commit**

```bash
git add sql/mall_attr_front_category_schema.sql sql/mall_phase_b_*.sql README.md
git commit -m "feat(mall): Phase B 属性与前台类目 DDL/迁移/菜单"
```

---

### Task 2: 属性域 Domain / Mapper / Service / Controller

**Files:**
- Create under `backend/ruoyi-mall-product/src/main/java/com/ruoyi/mall/product/`:
  - `domain/MallAttr.java`, `MallAttrOption.java`, `MallCategoryAttr.java`
  - `mapper/MallAttrMapper.java`, `MallAttrOptionMapper.java`, `MallCategoryAttrMapper.java`
  - `dto/MallAttrSaveRequest.java`, `MallAttrOptionSaveRequest.java`, `MallCategoryAttrBindRequest.java`（含 `List<Item>`：attrId, attrType, required, sort）
  - `vo/MallAttrVO.java`, `MallAttrTemplateVO.java`（含 saleAttrs/descAttrs，每项带 options）
  - `service/MallAttrService.java` + `impl/MallAttrServiceImpl.java`
  - `controller/MallAttrController.java`
- Modify: `constant/MallProductConstants.java` — 增加 `ATTR_TYPE_SALE`/`DESC`、`INPUT_TYPE_*`
- Modify: `controller/MallCategoryController.java` — 增加 attrs 与 attr-template 端点（或独立 controller 同路径前缀）

**Interfaces:**
- Produces:
  - `MallAttrService.page/list/get/create/update/delete`
  - `MallAttrService.listOptions(Long attrId)` / `replaceOptions(Long attrId, List<...>)`
  - `MallAttrService.listCategoryAttrs(Long categoryId)` / `replaceCategoryAttrs(Long categoryId, List<...>)`
  - `MallAttrService.getAttrTemplate(Long categoryId)` → `MallAttrTemplateVO`
- Consumes: 现有 `MallCategoryMapper` 校验类目存在；绑定时校验叶子（无子类目）

- [ ] **Step 1: 常量与实体 Mapper**

风格对齐 `MallBrand`（`@TableName`、`@Data`、Lombok）。

- [ ] **Step 2: MallAttrServiceImpl CRUD + options 全量替换**

options：`delete` by attr_id 再 batch insert。属性删除前检查 `mall_category_attr` 引用。

- [ ] **Step 3: 类目绑定 + attr-template**

`replaceCategoryAttrs`：校验 category 为叶子；attr_type 仅 SALE|DESC；全量删插。  
`getAttrTemplate`：join attr + options（status=0），按 sort 排序，分组 sale/desc。

- [ ] **Step 4: Controller**

```
GET/POST/PUT/DELETE /mall/attr
GET/PUT /mall/attr/{id}/options
GET/PUT /mall/category/{id}/attrs
GET /mall/category/{id}/attr-template   // 允许 mall:spu:add|edit 访问
```

权限：`@PreAuthorize` 对齐 brand/category 模式。

- [ ] **Step 5: Compile**

```bash
export JAVA_HOME=.../corretto-17...
cd backend && mvn -B -DskipTests compile -pl ruoyi-mall-product -am -q
```

Expected: `BUILD SUCCESS` / exit 0

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(mall): 属性库与类目属性绑定 API"
```

---

### Task 3: 前台类目 Domain / Service / Controller

**Files:**
- Create: `domain/MallFrontCategory.java`, `MallFrontCategoryRel.java`
- Create: mappers, `dto/MallFrontCategorySaveRequest.java`, `MallFrontCategoryQuery.java`, `vo/MallFrontCategoryTreeVO.java`
- Create: `service/MallFrontCategoryService.java` + impl
- Create: `controller/MallFrontCategoryController.java`

**Interfaces:**
- Produces:
  - `tree` / `create` / `update` / `delete`（有子节点或有 rel 时的删除策略：有子不可删；有 rel 先清或拒绝——采用**有子不可删，删除时级联删 rel**）
  - `listRels(frontId)` / `replaceRels(frontId, List<Long> backCategoryIds)` — 仅允许后台叶子 id
  - `listActiveTree()` — 公开用 status=0
  - `resolveBackCategoryIds(Long frontCategoryId)` — 该节点及其子树映射的全部 back id（去重）；若仅映射在叶子前台，则查该 front_id 的 rel；**约定：筛选时取该前台节点及其所有后代 front 的 rel 并集**

- [ ] **Step 1: 实体与 CRUD 树**（对齐 `MallCategoryServiceImpl`）

- [ ] **Step 2: rel 替换与 resolveBackCategoryIds**

```java
// resolve: 收集 frontId 子树所有 front ids，再 select rel where front_id in (...)
Set<Long> resolveBackCategoryIds(Long frontCategoryId);
```

- [ ] **Step 3: Controller `/mall/front-category`**

含 `GET ?tree=true`、`GET/PUT /{id}/rels`、`GET /options`（若需要）。

- [ ] **Step 4: Compile + Commit**

```bash
git commit -m "feat(mall): 前台类目与后台映射 API"
```

---

### Task 4: SPU 保存/详情接入属性校验与 DESC 落库

**Files:**
- Modify: `dto/MallSpuSaveRequest.java` — 增加 `List<MallSpuAttrValueRequest> attrValues`（attrId, value）
- Create: `dto/MallSpuAttrValueRequest.java`
- Create: `domain/MallSpuAttrValue.java` + mapper
- Modify: `vo/MallSpuDetailVO.java` — 增加 `List<MallSpuAttrValueVO> attrValues`（attrId, attrName, value, attrType）
- Modify: `service/impl/MallSpuServiceImpl.java` — save/detail/publicDetail/publish 校验
- Modify: `service/impl` 中 publicPage：当 public 且 categoryId 非空时，经 `MallFrontCategoryService.resolveBackCategoryIds` 后 `in (category_id)`；若解析为空则返回空页

**Interfaces:**
- Consumes: `MallAttrService.getAttrTemplate`、`MallFrontCategoryService.resolveBackCategoryIds`
- Produces: 保存时写 `mall_spu_attr_value`；详情带描述属性

- [ ] **Step 1: 叶子类目校验**

保存时：`selectCount` 子类目 == 0，否则 `ServiceException("请选择后台叶子类目")`。

- [ ] **Step 2: DESC / SALE 校验与落库**

伪代码：

```java
MallAttrTemplateVO template = mallAttrService.getAttrTemplate(categoryId);
// DESC required → attrValues 必须有非空 value
// 全量：delete by spu_id + insert
// if template.saleAttrs not empty:
//   for each enabled sku: parse specs_json Map; keys must contain all sale attr names;
//   select/multi values must ∈ options
// else: skip sale validation
```

用 Jackson/`ObjectMapper` 或项目已有 JSON 工具解析 `specs_json`；非法 JSON 抛业务异常。

- [ ] **Step 3: publicPage 前台类目过滤**

```java
if (publicOnly && query.getCategoryId() != null) {
  Set<Long> backIds = frontCategoryService.resolveBackCategoryIds(query.getCategoryId());
  if (backIds.isEmpty()) { return empty page; }
  wrapper.in(MallSpu::getCategoryId, backIds);
} else if (query.getCategoryId() != null) {
  wrapper.eq(MallSpu::getCategoryId, query.getCategoryId()); // admin 仍后台 id
}
```

注意：admin `page` 与 `publicPage` 分支勿混用。

- [ ] **Step 4: Public categories API**

`PublicMallProductController.categories` → `mallFrontCategoryService.listActiveTree()`（或 listActive 扁平+前端建树；优先树）。

- [ ] **Step 5: Compile + Commit**

```bash
git commit -m "feat(mall): SPU 属性校验与前台类目公开筛选"
```

---

### Task 5: 前端 API + 属性管理 + 前台类目页

**Files:**
- Create: `frontend/src/api/mall/attr.js`, `frontend/src/api/mall/frontCategory.js`
- Create: `frontend/src/views/mall/admin/attr/index.vue`
- Create: `frontend/src/views/mall/admin/frontCategory/index.vue`
- Modify: `frontend/src/views/mall/admin/category/index.vue` — 标题「后台类目」；增加「绑定属性」对话框（拉/存 `/mall/category/{id}/attrs`）

**Interfaces:**
- Consumes: Task 2/3 HTTP API

- [ ] **Step 1: API 封装**（对齐 `category.js`/`brand.js`）

- [ ] **Step 2: 属性管理页**

列表 + 弹窗编辑 name/inputType/status/sort；子表或抽屉维护 options。

- [ ] **Step 3: 前台类目页**

树表 CRUD；「映射」弹窗多选后台叶子（`listMallCategoryOptions` 展平叶子）。

- [ ] **Step 4: 后台类目绑定属性 UI**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(mall): 属性与前台类目管理后台页"
```

---

### Task 6: 前端 SPU 发品 + C 端适配

**Files:**
- Modify: `frontend/src/views/mall/admin/spu/index.vue`
- Modify: `frontend/src/api/mall/spu.js`（若需 template 调用可放 category/attr API）
- Modify: `frontend/src/views/public/mall/list.vue`, `home.vue`, `detail.vue`
- Modify: `frontend/src/api/mall/public.js`（无需改 URL，语义已变）

**Interfaces:**
- Consumes: `GET /mall/category/{id}/attr-template`；保存带 `attrValues`

- [ ] **Step 1: SPU 表单**

选中 `categoryId` 后请求 template；渲染 DESC 表单项；SALE：多选各属性值 →「生成 SKU」按钮做前端笛卡尔积填入 `skus[].specsJson`（JSON.stringify）；仍可手改价格库存。

- [ ] **Step 2: C 端 list/home**

类目数据改为前台树（若 API 返回树，侧栏递归或扁平化展示）；`categoryId` 传前台 id。

- [ ] **Step 3: C 端 detail**

展示 `attrValues`（描述参数表）。

- [ ] **Step 4: 手动冒烟清单**（无自动化测试框架时）

1. 执行 SQL 脚本  
2. 后台配置属性并绑定叶子  
3. 新发品缺 SALE 被拒；补全后上架成功  
4. C 端前台类目筛选有结果；详情有 DESC  
5. 无绑定类目旧商品仍可编辑上架  

- [ ] **Step 5: Compile backend + Commit**

```bash
git commit -m "feat(mall): 发品属性模板与 C 端前台类目导购"
```

---

### Task 7: 设计状态与实现资料同步

**Files:**
- Modify: `docs/superpowers/specs/2026-07-20-mall-product-phase-b-design.md` — Status → Approved / Implemented
- Optional: `docs/ecommerce-impl/` 增加 phase1.5 或 phase-b 说明（若已有惯例则跟随；否则仅更新 design Status）

- [ ] **Step 1: 更新 spec Status 与验收勾选**
- [ ] **Step 2: Commit**

```bash
git commit -m "docs(mall): 标记 Phase B 设计已落地"
```

---

## Spec coverage checklist

| Spec 项 | Task |
|---------|------|
| DDL 属性/前台/rel/spu_attr_value | 1 |
| 迁移 1:1 前台 | 1 |
| 菜单权限 | 1 |
| 属性 CRUD/options/绑定/template | 2 |
| 前台类目 CRUD/rels/resolve | 3 |
| SPU 校验+DESC+公开筛选/类目 | 4 |
| Admin 属性/前台/绑定 UI | 5 |
| 发品 UI + C 端 | 6 |
| 文档收尾 | 7 |

## Placeholder scan

无 TBD；类型名与路径与现有 `MallBrand*` / `MallCategory*` 对齐。
