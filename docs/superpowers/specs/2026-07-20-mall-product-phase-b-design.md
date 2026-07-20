# Design: Mall Product Phase B（属性化 + 前后台类目）

**Status**: Draft — awaiting user review of this file  
**Author**: Alex / Cursor  
**Last Updated**: 2026-07-20  
**Branch**: `feat/mall-product-a-b-c`  
**Depends on**: Phase A (`a266ce9`)  
**Stakeholders**: Eng

---

## 1. Problem Statement

Phase 1 商品域已具备 SPU/SKU/单套类目/简单库存，但规格靠自由文本 `specs_json`、类目前后台不分，运营无法按类目约束发品，C 端也无法用稳定的前台导航挂多后台叶子。

**Evidence**: 仓库 `ruoyi-mall-product` 无属性表；`mall_category` 同时服务运营与公开列表；业界惯例为后台类目挂属性 + 前台类目导购。

---

## 2. Goals & Success Metrics

| Goal | Metric / Acceptance |
|------|---------------------|
| 可运营属性 | 可 CRUD 属性与选项，并绑定到后台叶子类目（SALE/DESC） |
| 前后台类目分离 | 前台树可维护；可映射多个后台叶子；C 端只读前台 |
| 发品结构化 | 有 SALE 绑定时，SKU specs 与模板校验；DESC 落库并可在详情展示 |
| 兼容旧数据 | 无属性绑定的类目/旧 SPU 仍可保存与上架 |

**Non-Goals**
- 关键属性 / CSPU / 属性审核流
- 服务端强制笛卡尔积落库（仅前端辅助生成 SKU 行）
- 库存中心、ES facet、销量（Phase C）
- 独立 PIM 服务

---

## 3. Approach（已确认：方案 A）

- 现有 `mall_category` **升级为后台类目**（表名不改，语义改为 back）
- 新建前台类目 + 映射表
- 新建属性库 / 选项 / 类目属性绑定 / SPU 描述属性值
- SKU 继续用 `specs_json` 存销售属性快照
- SPU.`category_id` 仍指向 **后台** 类目

不采用：重建 `mall_back_category` 大迁移（方案 B）；不上类 PIM（方案 C）。

---

## 4. Data Model

### 4.1 后台类目（现有）

`mall_category` — 不变结构；运营发品、属性挂载、SPU 绑定均使用此表。发品须选 **叶子**（无子节点）。

### 4.2 前台类目

```text
mall_front_category
  id, parent_id, name, sort, status, icon, audit fields, remark

mall_front_category_rel
  id, front_id, back_category_id
  UNIQUE(front_id, back_category_id)
```

语义：一个前台节点可映射多个后台叶子；公开列表按前台 id 展开为后台 id 集合后过滤 SPU。

### 4.3 属性

```text
mall_attr
  id, name, input_type(text|select|multi), status, sort, audit, remark

mall_attr_option
  id, attr_id, value, sort, status

mall_category_attr
  id, category_id(后台叶子), attr_id, attr_type(SALE|DESC), required(0/1), sort
  UNIQUE(category_id, attr_id)

mall_spu_attr_value
  id, spu_id, attr_id, value(varchar)
  UNIQUE(spu_id, attr_id)
```

### 4.4 兼容字段

- `mall_sku.specs_json`：SALE 属性名→值的 JSON 快照；有 SALE 模板时后端校验键集合与可选值
- 旧数据不强制回填 `mall_spu_attr_value` 或 attr id

---

## 5. API Contract

### 5.1 Admin（需登录 + 权限）

| Method | Path | Purpose |
|--------|------|---------|
| CRUD | `/mall/attr` | 属性 |
| GET/PUT | `/mall/attr/{id}/options` | 选项全量查询/替换 |
| GET/PUT | `/mall/category/{id}/attrs` | 类目属性绑定全量查询/替换 |
| GET | `/mall/category/{id}/attr-template` | 发品模板（SALE/DESC + options） |
| CRUD + tree | `/mall/front-category` | 前台类目 |
| GET/PUT | `/mall/front-category/{id}/rels` | 映射后台叶子 |
| POST | `/mall/spu` | 扩展：写 DESC 值 + SALE 校验（见 §6） |

权限前缀：`mall:attr:*`、`mall:frontCategory:*`；类目绑定复用/扩展 `mall:category:edit`。

### 5.2 Public

| Method | Path | Change |
|--------|------|--------|
| GET | `/public/mall/categories` | 返回启用前台类目（树或带 children） |
| GET | `/public/mall/spus` | `categoryId` = **前台**类目 id → 映射后台叶子 IN 查询 |
| GET | `/public/mall/spus/{id}` | 详情增加描述属性列表 |

**Breaking**：公开类目 id 迁移后与旧后台 id 不一定相同；本仓库 C 端与迁移脚本一并切换。

---

## 6. Publish / Save Rules

1. SPU 必须绑定后台 **叶子**类目。  
2. 若该类目存在 `mall_category_attr`：  
   - DESC：必填项写入 `mall_spu_attr_value`；全量以请求为准（可先删后插）  
   - SALE：每个 `status=0` 的 SKU，`specs_json` 必须包含全部 SALE 属性名；`select`/`multi` 值须属于启用 option  
3. 若类目 **无** SALE 绑定：允许自由 `specs_json`（兼容演示商品）。  
4. 上架 `ON`：≥1 启用 SKU + 上述校验。  
5. 服务端 **不**自动生成笛卡尔积 SKU；Admin UI 可提供「按销售属性生成行」辅助，提交仍走现有嵌套 SKU。

---

## 7. Migration

1. `mall_category` 数据保留为后台类目。  
2. 一次性脚本：为每个现有类目创建同名 `mall_front_category`（保持 parent 结构），并插入 1:1 `mall_front_category_rel`。  
3. 可选 demo：为某一叶子绑定「颜色」「尺码」SALE 属性及选项。  
4. 菜单种子：属性管理、前台类目管理。

脚本位置：`sql/mall_attr_front_category_schema.sql` + `sql/mall_phase_b_migrate_front_category.sql`（及菜单 seed）；设计副本可同步 `docs/ecommerce-impl/`（实现时）。

---

## 8. Frontend Impact

| Area | Change |
|------|--------|
| 新页 | 属性管理；前台类目（含映射） |
| 后台类目页 | 文案「后台类目」；绑定属性入口 |
| SPU 编辑 | 选类目 → 拉 template → DESC 表单 + SALE 生成/编辑 SKU |
| C 端 list/home | 前台树；categoryId 为前台 id |
| C 端 detail | 描述属性参数展示 |

---

## 9. Module Placement

全部落在现有 `ruoyi-mall-product`（不新建 Maven 模块）。`ruoyi-mall-trade` 仅消费 SKU，原则上不改；若公开类目语义变化不影响下单。

---

## 10. Testing / Verification

- Maven：`mvn -pl ruoyi-mall-product -am -DskipTests compile`（JDK 17）  
- 冒烟：属性绑定 → 发品校验拒绝错误 specs → 正确上架 → C 端前台类目筛选 → 详情 DESC  
- 回归：无属性类目下旧流程仍可用  

---

## 11. Open Questions（已关闭）

| Q | Decision |
|---|----------|
| B 范围 | 完整版：属性 + 前后台类目（用户选 2） |
| 类目迁移策略 | 方案 A：旧表变后台 |
| 关键属性 | 本 Phase 不做 |

---

## 12. Next Step After Spec Approval

Invoke writing-plans → `docs/superpowers/plans/2026-07-20-mall-product-phase-b.md` → 按任务实现。
