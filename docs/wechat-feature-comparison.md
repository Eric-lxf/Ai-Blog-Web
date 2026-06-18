# 微信公众号运营功能对比文档

> 对比基准：**正常公众号日常运营**所需能力 vs **本项目 `ruoyi-wechat` 模块**当前实现  
> 文档日期：2026-06-18  
> 代码分支：`cursor/fix-wechat-menu-publish-3eae`（含菜单修复、发布能力、带参二维码等近期改动）

## 图例

| 标记 | 含义 |
|------|------|
| ✅ | 已实现，可日常使用 |
| 🟡 | 部分实现 / 仅内部链路 / 体验不完整 |
| ❌ | 未实现 |
| ➖ | 本项目明确不在范围（v1 设计排除） |

---

## 一、总览

| 维度 | 运营典型需求项 | 已实现 | 部分实现 | 未实现 |
|------|----------------|--------|----------|--------|
| 基础接入 | 6 | 4 | 1 | 1 |
| 内容发布 | 10 | 5 | 3 | 2 |
| 粉丝用户 | 7 | 2 | 1 | 4 |
| 互动客服 | 8 | 3 | 1 | 4 |
| 菜单入口 | 6 | 3 | 1 | 2 |
| 数据统计 | 5 | 0 | 1 | 4 |
| 扩展能力 | 6 | 0 | 0 | 6 |
| **合计** | **48** | **17** | **8** | **23** |

**结论（一句话）**：本项目已覆盖「**多账号接入 + 博客发文同步 + 菜单/二维码/被动回复 + 粉丝与消息日志**」这条**技术型内容号**主链路；距离完整公众号运营后台，主要缺口在 **素材库独立管理、草稿箱运营、主动触达（客服/模板/群发）、标签分群、数据报表**。

---

## 二、分模块对比

### 2.1 基础接入与配置

| 功能 | 运营场景 | 状态 | 现有实现说明 |
|------|----------|------|----------------|
| 多账号 AppID/Secret 管理 | 管理多个公众号凭证 | ✅ | `账号管理`：增删改查、启用停用 |
| 回调 URL 配置指引 | 在微信后台填服务器地址 | ✅ | 账号页展示 `/public/wechat/callback/{accountId}` |
| 服务器校验（GET） | 微信验证开发者服务器 | ✅ | `PublicWechatController` 签名校验 |
| 消息接收（POST） | 接收用户消息与事件 | ✅ | 明文 + AES 兼容，写消息日志 |
| access_token 缓存 | 调用微信 API | ✅ | Redis 缓存，`WechatTokenService` |
| 连接测试 | 确认凭证可用 | ✅ | `POST /wechat/account/{id}/test` |
| 功能总开关 | 一键关闭微信能力 | 🟡 | `wechat.enabled` 已入库，**控制器未统一拦截** |
| 独立配置页 | 运营人员可视化改开关 | ❌ | 设计有 `config.vue`，未实现；需改 `sys_config` |
| 默认账号 | 减少前端每次选号 | 🟡 | `wechat.defaultAccountId` 配置项存在，前端未全局默认 |

**对应页面**：`wechat/account/index`  
**对应表**：`wx_account`

---

### 2.2 内容生产与发布

| 功能 | 运营场景 | 状态 | 现有实现说明 |
|------|----------|------|----------------|
| 博客一键推送到公众号 | 文章写完同步发文 | ✅ | 文章编辑页「推送到公众号」 |
| Markdown → 微信 HTML | 正文格式兼容 | ✅ | commonmark + Jsoup 清洗 |
| 封面上传 thumb_media_id | 草稿必填封面 | ✅ | 推送时自动上传封面 |
| 正文图片上传微信 CDN | 图文内图片可显示 | ✅ | 推送流水线内自动替换 `img src` |
| 创建草稿 `draft/add` | 先存草稿箱 | ✅ | 推送流程内调用，非独立运营 |
| 发布草稿 `freepublish/submit` | 正式发布 | ✅ | 支持「仅草稿 / 草稿并发布」 |
| 已发布列表 `batchget` | 查看线上文章 | ✅ | 推送记录页「微信已发布消息」Tab |
| 发布状态查询 `freepublish/get` | 查异步发布结果 | ✅ | 本地记录「同步状态」+ 直接查微信 |
| 获取已发布图文 `getarticle` | 查看详情 | ✅ | 已发布列表「详情」 |
| 删除已发布 `freepublish/delete` | 下线文章 | ✅ | 已发布列表「删除」 |
| 本地推送记录 | 审计、排错 | ✅ | `wx_publish_record`，含状态机 |
| 从记录重新发布 | 失败重试 | 🟡 | `POST /wechat/publish/{id}/submit`，非设计中的 `retry` 命名 |
| **草稿箱独立管理** | 在微信侧增删改草稿 | ✅ | `draft/batchget`、`get`、`add`、`update`、`delete`、`count` |
| **素材库独立上传** | 运营主动上传图片/封面/正文图 | ✅ | `POST /wechat/material/upload`，类型 thumb/image/content |
| **永久素材列表/删除** | 管理微信素材库 | 🟡 | 本地 `wx_media_asset` + 微信 `batchget_material` 查询 |
| 留言/评论管理 | 读者互动 | ❌ | 未对接评论相关 API |
| 阅读原文链接配置 | 引流回官网 | 🟡 | 字段存在，推送时固定空字符串 |

**对应页面**：`wechat/publish/index`、`wechat/material/index`、博客 `article/edit`  
**已对接微信 API**：`draft/add`、`freepublish/*`、`material/add_material`（内部）、`media/uploadimg`（内部）

---

### 2.3 粉丝与用户运营

| 功能 | 运营场景 | 状态 | 现有实现说明 |
|------|----------|------|----------------|
| 粉丝列表 | 查看关注用户 | ✅ | 分页、按账号/状态/昵称筛选 |
| 全量粉丝同步 | 从微信拉取粉丝 | ✅ | `user/get` + `user/info/batchget` |
| 48001 降级 | 个人号无列表权限 | ✅ | 自动改从消息日志补全粉丝 |
| 关注/取关事件入库 | 实时更新粉丝状态 | ✅ | 回调触发 `handleSubscribeEvent` |
| 用户备注 | CRM 式标注用户 | 🟡 | 库表有字段，同步时写入；**无单独编辑 UI** |
| 用户标签 | 分群运营、精准推送 | ❌ | 未对接 `user/tag/*` |
| 黑名单管理 | 屏蔽恶意用户 | ❌ | 未对接 |
| 粉丝画像/地域统计 | 了解受众结构 | ❌ | 未对接数据统计 API |

**对应页面**：`wechat/fans/index`  
**对应表**：`wx_fans`

---

### 2.4 互动与客服

| 功能 | 运营场景 | 状态 | 现有实现说明 |
|------|----------|------|----------------|
| 关注自动回复 | 新关注欢迎语 | ✅ | 本地规则 `reply_type=subscribe`，回调被动回复 |
| 关键词自动回复 | 常见问题 | ✅ | 包含/全等匹配 |
| 默认回复 | 未命中关键词时 | ✅ | `reply_type=default` |
| 扫码回复 | 渠道码不同欢迎语 | ✅ | `reply_type=scan`，按场景值匹配 |
| 消息日志 | 追溯用户说了什么 | ✅ | 入站消息全量记录 |
| 回复规则管理 | 配置上述规则 | 🟡 | 有增改列表，**无删除接口/UI**；不同步微信后台自动回复页 |
| 仅文本回复 | 图文/图片/语音回复 | ❌ | 被动回复仅组装 text XML |
| 客服消息（主动） | 48 小时内主动联系用户 | ❌ | 无 `message/custom/send` |
| 模板消息 / 订阅通知 | 订单、通知类触达 | ❌ | 未对接 |
| 群发消息 | 运营公告推送 | ❌ | 未对接 `message/mass/*` |

**对应页面**：`wechat/reply/index`、`wechat/message/index`  
**说明**：自动回复走**开发者回调**，与微信公众平台后台「自动回复」页面互不影响，需在自有后台配置。

---

### 2.5 菜单与入口

| 功能 | 运营场景 | 状态 | 现有实现说明 |
|------|----------|------|----------------|
| 创建自定义菜单 | 底部菜单栏 | ✅ | `menu/create`，修复了 47001 格式问题 |
| 查询微信当前菜单 | 与线上一致性核对 | ✅ | `menu/get` |
| 删除微信菜单 | 清空菜单 | ✅ | `menu/delete` |
| 本地菜单配置管理 | 保存草稿再发布 | ✅ | JSON 编辑 + 发布 |
| 带参二维码生成 | 渠道统计、扫码关注 | ✅ | `qrcode/create`，临时/永久、整型/字符串场景 |
| 扫码次数统计 | 渠道效果 | ✅ | 回调累加 `scan_count` |
| 可视化菜单编辑器 | 非技术人员配置 | ❌ | 当前为 JSON 文本框 |
| 菜单同步到本地 | 从微信拉取覆盖本地 | ❌ | 可查微信菜单，未一键同步入库 |
| 个性化菜单 | 不同用户看不同菜单 | ❌ | 未对接 `menu/addconditional` 等 |
| 二维码跳小程序 | 扫码直开小程序 | ❌ | **明确不做** `qrcodejump`（当前阶段） |

**对应页面**：`wechat/menu/index`、`wechat/qrcode/index`  
**对应表**：`wx_menu`、`wx_qrcode`

---

### 2.6 数据统计与分析

| 功能 | 运营场景 | 状态 | 现有实现说明 |
|------|----------|------|----------------|
| 用户增减数据 | 看涨粉趋势 | ❌ | 未对接 `datacube` 用户分析 |
| 图文阅读/分享 | 看文章效果 | ❌ | 未对接图文分析 |
| 消息收发统计 | 看互动量 | ❌ | 未对接 |
| 接口调用统计 | 排障 | ❌ | 未对接 |
| 本地扫码/推送统计 | 简易运营指标 | 🟡 | 渠道码 `scan_count`、推送记录状态，**无汇总报表** |

---

### 2.7 扩展能力（正常大号常见，本项目 v1 排除）

| 功能 | 状态 | 说明 |
|------|------|------|
| 微信支付 | ➖ | 设计明确 out of scope |
| 小程序管理/关联 | ➖ | 未做；与 qrcodejump 相关 |
| 企业微信 | ➖ | 未做 |
| 网页授权 OAuth | ❌ | 未做 H5 拉取用户信息 |
| JS-SDK 分享 | ❌ | 未做 |
| 多账号编排/工作流 | ➖ | 设计排除 |

---

## 三、后台菜单与页面对照

| 菜单 | 路径 | 权限 | 实现度 |
|------|------|------|--------|
| 账号管理 | `wechat/account` | `wechat:account:*` | ✅ 完整 |
| 推送记录 | `wechat/publish` | `wechat:publish:*` | ✅ 完整（含微信已发布 Tab） |
| 素材管理 | `wechat/material` | `wechat:material:list/remove` | 🟡 仅本地推送产物列表 |
| 菜单管理 | `wechat/wx-menu` | `wechat:menu:*` | ✅ 可用（JSON 编辑） |
| 自动回复 | `wechat/reply` | `wechat:reply:*` | 🟡 缺删除 |
| 粉丝管理 | `wechat/fans` | `wechat:fans:list` | ✅ 列表 + 同步 |
| 消息日志 | `wechat/message-log` | `wechat:message:list` | ✅ 只读 |
| 渠道二维码 | `wechat/qrcode` | `wechat:qrcode:*` | ✅ 完整 |
| 公众号配置页 | — | — | ❌ 未实现 |

---

## 四、已对接微信 API 清单

| API | 用途 | 暴露方式 |
|-----|------|----------|
| `GET /cgi-bin/token` | 获取 access_token | 内部 |
| `POST /cgi-bin/draft/add` | 新增草稿 | 推送流水线 |
| `POST /cgi-bin/freepublish/submit` | 发布草稿 | 推送 / 管理 API |
| `POST /cgi-bin/freepublish/batchget` | 已发布列表 | 管理 API |
| `POST /cgi-bin/freepublish/get` | 发布状态 | 管理 API |
| `POST /cgi-bin/freepublish/getarticle` | 已发布详情 | 管理 API |
| `POST /cgi-bin/freepublish/delete` | 删除已发布 | 管理 API |
| `POST /cgi-bin/material/add_material` | 上传封面永久素材 | 推送流水线 |
| `POST /cgi-bin/media/uploadimg` | 上传正文图片 | 推送流水线 |
| `POST /cgi-bin/menu/create` | 创建菜单 | 管理 API |
| `GET /cgi-bin/menu/get` | 查询菜单 | 管理 API |
| `GET /cgi-bin/menu/delete` | 删除菜单 | 管理 API |
| `POST /cgi-bin/qrcode/create` | 生成带参码 | 管理 API |
| `GET /cgi-bin/user/get` | 粉丝 OpenID 列表 | 粉丝同步 |
| `POST /cgi-bin/user/info/batchget` | 批量粉丝信息 | 粉丝同步 |
| `GET /cgi-bin/user/info` | 单个粉丝信息 | 关注事件 |

---

## 五、与设计文档差异（`docs/superpowers/specs/2026-06-07-wechat-official-account-design.md`）

| 设计项 | 设计状态 | 当前状态 |
|--------|----------|----------|
| `POST /wechat/publish/retry/{id}` | 规划 | 🟡 以 `/publish/{id}/submit` 实现 |
| `POST /wechat/material/upload` | 规划 | ❌ 仅内部上传 |
| `POST /wechat/menu/sync/{accountId}` | 规划 | ❌ |
| `POST /wechat/reply/delete/{id}` | 规划 | ❌ |
| `views/wechat/config.vue` | 规划 | ❌ |
| 带参二维码 | 未写入 v1 设计 | ✅ 已实现 |
| 发布能力 5 接口 | 未写入 v1 设计 | ✅ 已实现 |
| qrcodejump | — | ❌ 明确不做 |

---

## 六、按运营角色推荐阅读路径

### 内容运营（发文为主）

```
博客写文章 → 推送到公众号 → 推送记录查状态 → 微信已发布消息管理
```
**缺口**：草稿箱独立编辑、素材库上传、留言管理。

### 用户运营（涨粉与互动）

```
渠道二维码 → 自动回复(扫码/关注) → 粉丝列表/同步 → 消息日志
```
**缺口**：标签分群、群发、模板消息、数据报表。

### 技术运营（接入维护）

```
账号管理 → 回调配置 → 测试连接 → 菜单发布 → 消息日志排障
```
**缺口**：功能开关未强制、无独立配置页。

---

## 七、建议优先级（若继续迭代）

| 优先级 | 功能 | 理由 |
|--------|------|------|
| P0 | 素材库独立上传与管理 | 运营不依赖博客也能配图发文 |
| P0 | 草稿箱 CRUD（`draft/batchget` 等） | 补全内容运营闭环 |
| P1 | 自动回复规则删除 | 基础 CRUD 完整性 |
| P1 | `wechat.enabled` 网关拦截 + 配置页 | 运维可控 |
| P1 | 菜单可视化编辑器 / 同步到本地 | 降低配置门槛 |
| P2 | 用户标签 + 客服消息 | 精细化运营 |
| P2 | 模板消息 / 群发 | 主动触达 |
| P3 | 数据统计 datacube | 报表分析 |
| 暂缓 | qrcodejump、支付、小程序 | 按业务需要再开 |

---

## 八、相关文件索引

| 类型 | 路径 |
|------|------|
| 后端模块 | `backend/ruoyi-wechat/` |
| 数据库 | `sql/wechat_schema.sql` |
| 增量 SQL | `sql/wechat_menu_permissions.sql`、`sql/wechat_publish_permissions.sql`、`sql/wechat_qrcode_schema.sql` |
| 前端页面 | `frontend/src/views/wechat/` |
| 设计文档 | `docs/superpowers/specs/2026-06-07-wechat-official-account-design.md` |
| 实施计划 | `docs/superpowers/plans/2026-06-07-wechat-official-account.md` |
