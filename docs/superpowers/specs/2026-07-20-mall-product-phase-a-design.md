# Design: Mall Product Phase A（加固 MVP）

**Status**: Approved (user 确认 A→B→C，并同意先做 A)  
**Branch**: `feat/mall-product-a-b-c`  
**Date**: 2026-07-20

## Problem

C 端列表传 `keyword`/`sort`，后台商品列表传 `brandId`，但 `MallSpuPageQuery` 与 `querySpus` 未承接，导致筛选/排序无效或静默忽略。

## Goals

- 公开列表：关键词搜索生效；`latest` / `price` 排序生效
- 运营列表：品牌筛选生效
- 不引入属性体系、库存中心、ES（属 B/C）

## Non-Goals

- 销量排序真实数据（无销量字段；UI 去掉「销量优先」）
- SKU 独立 Controller、库存预占、前后台类目分离

## Solution

1. `MallSpuPageQuery` 增加 `keyword`、`brandId`、`sort`
2. `querySpus`：`keyword`/`name` 模糊匹配名称；`brandId` 等值；`sort` 白名单（`latest`|`price`，非法回退默认）
3. 公开默认/latest：按 `update_time DESC, id DESC`；price：按启用 SKU 最低价 ASC
4. 运营列表仍按 `sort ASC, update_time DESC`（未传 sort 时）
5. C 端去掉「销量优先」选项

## Acceptance

- [x] `GET /public/mall/spus?keyword=xx` 按名称过滤
- [x] `sort=latest|price` 排序符合预期（代码已实现；需联调冒烟）
- [x] 后台 `brandId` 过滤生效
- [x] `mvn -pl ruoyi-mall-product -am -DskipTests compile` 通过
