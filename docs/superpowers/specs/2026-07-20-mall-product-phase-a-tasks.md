# Tasks: Mall Product Phase A

> 对应 spec: `docs/superpowers/specs/2026-07-20-mall-product-phase-a-design.md`

## Task 1: 扩展查询 DTO 与 querySpus 筛选/排序

- [x] `MallSpuPageQuery` 增加 `keyword`、`brandId`、`sort`
- [x] `MallSpuServiceImpl#querySpus` 支持 keyword/name、brandId、sort 白名单
- **Files**: `MallSpuPageQuery.java`, `MallSpuServiceImpl.java`

## Task 2: C 端列表去掉无效销量排序

- [x] `list.vue` 移除「销量优先」；保留 latest / price
- **Files**: `frontend/src/views/public/mall/list.vue`

## Task 3: 编译验证

- [x] `mvn -pl ruoyi-mall-product -am -DskipTests compile`（JDK 17）
