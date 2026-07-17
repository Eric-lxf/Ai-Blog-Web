# 项目重命名计划：从 Ai-Blog-Web 到电商平台新名字

**状态**：Draft
**作者**：Cloud Agent（架构视角）
**最后更新**：2026-07-17
**前置文档**：`docs/ecommerce-platform-transformation-plan.md`
**范围**：命名推荐 + 全仓库改名影响面盘点 + 分风险等级的彻底执行计划 + 数据库改名方案 + 回滚方案

---

## 1. 背景

当前名字 `Ai-Blog-Web` / 数据库 `ai_blog` 在电商改造后会变成"名字撞不上业务"——项目会有十几个交易域子系统，博客只是其中一个内容子模块。本文给出**彻底重命名**的完整计划：不止改仓库标题，而是把散落在 GitHub 仓库名、前端包名、后端应用名、Docker 镜像/Registry、CI/CD、生产部署路径、数据库名这 8+ 个层级里的旧名字**一次性对齐**到新名字。

> 本文只输出计划。是否执行、执行到哪个 Tier，需要你确认后我再单独开分支落地（改名类变更不与电商功能开发混在同一个 PR 里）。

---

## 2. 名字推荐（5 个候选）

| 候选 | 英文/代号 | 命名解读 | 优势 | 顾虑 |
|---|---|---|---|---|
| **AI-Mall-Web** | `ai-mall-web` | 保持现有 `Ai-Blog-Web` 命名规则，仅把 Blog 换成 Mall | 改动量最小、团队认知成本最低、CI/CD 变量名替换是纯字符串替换、不需要重新设计品牌 | 依然是"技术向"名字，对外品牌感一般 |
| **AI-Commerce** | `ai-commerce` | 强调"AI 驱动的电商平台"定位，不局限于"商城" | 更书面、更适合未来对外介绍/融资材料 | 与现有命名规则（Ai-Blog-Web 用的是"产品词"不是"行业词"）风格略有差异 |
| **NovaMall** | `nova-mall` | "Nova"=新星，脱离 Ai-/Blog 历史包袱，独立品牌感强 | 适合未来注册商标/域名对外使用；不绑定"AI"这个词（万一以后 AI 只是辅助功能而非卖点） | 改动最彻底，所有历史命名规则都要推翻重立 |
| **LingMall（灵犀商城）** | `ling-mall` | 中英结合，呼应"AI/灵性"意象，适合国内 To C 品牌调性 | 中文用户接受度高，适合做小程序/公众号品牌名 | 英文縮写在国际化场景下辨识度较低 |
| **OmniMall** | `omni-mall` | 强调多渠道覆盖（Web + 小程序 + App），对齐路线图里"复用微信渠道"的规划 | 名字本身传达"全渠道"定位，营销上有叙事空间 | "Omni"目前渠道能力尚未做到，名字略超前于现状 |

**推荐**：如果你还没有明确的对外品牌诉求，优先选 **`AI-Mall-Web`**（改名成本最低、风险最可控，适合"先把名字和现状对齐，以后有品牌需求再二次改名"）；如果这次要一步到位做成对外品牌，选 **`NovaMall`** 或 **`LingMall`**。

> 下文统一用 **`ai-mall`** 作为占位示例（对应候选 1），实际执行时把文中所有 `ai-mall` / `AI-Mall` / `AI_MALL` 替换成你最终选定的名字即可，替换规则不变。

---

## 3. 全仓库命名现状盘点（改名影响面）

以下是当前仓库里实际存在的名字散落情况（已逐一核实文件内容），彻底改名意味着要覆盖这 8 张表里的**每一行**：

| # | 层级 | 当前值 | 出现位置 |
|---|---|---|---|
| 1 | GitHub 仓库名 | `eric-lxf/ai-blog-web` | git remote |
| 2 | 前端包名/仓库地址 | `"name": "ai-blog-web"`、`repository.url: Eric-lxf/Ai-Blog-Web.git`、`"author": "Ai-Blog"` | `frontend/package.json`、`frontend/package-lock.json` |
| 3 | 前端页脚文案 | `Copyright © 2026 Ai-Blog-Web` | `frontend/src/settings.js` |
| 4 | 后端 Spring 应用名 | `name: Ai-Blog` | `backend/ruoyi-admin/.../application.yml` |
| 5 | 数据库名 | `ai_blog` | `sql/00-init-db.sql` 等 22 个 SQL 文件、`application-druid.yml`、`application-docker.yml`、`docker-compose.yml`、CI 里的 `MYSQL_DATABASE` 变量 |
| 6 | Docker 容器名 | `ai-blog-web-mysql/redis/backend/frontend` | `docker-compose.yml`、`docker-compose.prod.yml` |
| 7 | Docker 镜像名（CI 现用） | `ai-blog-backend`、`ai-blog-frontend`，namespace `blog_private` | `.github/workflows/build-push-acr.yml`、`deploy-ecs.yml` |
| 8 | Docker 镜像名（历史遗留，未清理） | `lxf_ai/al-blog-rouyi-web`、`al-blog-rouyi-server` | `scripts/release.ps1`、`README.md`、`frontend/Dockerfile`、`frontend/Dockerfile.release`、`backend/Dockerfile`（base image 引用） |
| 9 | 生产部署路径 | `/opt/ai-blog` | `.github/workflows/deploy-ecs.yml` 的 `ECS_DEPLOY_PATH`；服务器上的真实目录 |
| 10 | CI concurrency 分组名 | `ai-blog-production-deploy` | `.github/workflows/deploy-ecs.yml` |
| 11 | 业务相关环境变量前缀 | `BLOG_OSS_ENABLED`/`BLOG_OSS_KEY_ID`/`BLOG_OSS_KEY_SECRET`/`BLOG_OSS_BUCKET` | `docker-compose.prod.yml`、`deploy-ecs.yml`、GitHub Environment `production` 的 Secrets/Variables |
| 12 | 后端配置属性前缀 | `blog.oss.*` → 绑定到 `OssProperties`（`com.ruoyi.blog.config`） | `application.yml`/`application-docker.yml` 第 157/18 行 |
| 13 | Maven 业务模块与 Java 包名 | `artifactId: ruoyi-blog`，包名 `com.ruoyi.blog.*` | `backend/pom.xml`、`backend/ruoyi-blog/**`（全部 Java 文件） |
| 14 | 文档标题/正文 | `Ai-Blog-Web` | `README.md`、`AGENTS.md`、`docs/ecommerce-platform-transformation-plan.md` 等 |
| 15 | 前端零散引用 | `ai-blog-draft:`（本地草稿 localStorage key 前缀）、`ai-blog.zip`（代码生成下载文件名） | `useArticleDraft.js`、`views/tool/gen/index.vue` |

---

## 4. 按风险等级分 Tier 的彻底改名计划

> 遵循"先改零风险的，再改有回滚余地的，最后碰生产数据"的顺序。**Tier 0-2 建议一次性做完；Tier 3（数据库）单独挑窗口执行；Tier 4（Java 包名）默认不做，除非你明确要"连模块名都要改"。**

### 🟩 Tier 0 — 纯文档/纯文本，零运行时风险

| 改动项 | 操作 |
|---|---|
| `README.md` / `AGENTS.md` | 全文 `Ai-Blog-Web` → `AI-Mall-Web`，同步更新说明性文字 |
| `docs/ecommerce-platform-transformation-plan.md` 等既有文档 | 统一替换标题引用 |
| 前端页脚 `settings.js` | `Ai-Blog-Web` → `AI-Mall-Web` |
| `frontend/src/composables/useArticleDraft.js` 草稿 key 前缀 | `ai-blog-draft:` → `ai-mall-draft:`（注意：如果生产环境用户浏览器 localStorage 里已有旧 key 的草稿，改前缀会让旧草稿"读不到"，属于可接受的小成本，可在发布说明里提一句） |
| `views/tool/gen/index.vue` 生成下载文件名 | `ai-blog.zip` → `ai-mall.zip` |

**风险**：无。**回滚**：直接 `git revert`。

### 🟨 Tier 1 — 包名/应用名对齐（不涉及外部基础设施）

| 改动项 | 操作 |
|---|---|
| `frontend/package.json` | `name: "ai-blog-web"` → `"ai-mall-web"`；`repository.url` → 新 GitHub 地址；`author` → 按需更新 |
| `frontend/package-lock.json` | 随 `npm install` 自动同步（改完 package.json 后跑一次 `npm install` 重新生成 lock） |
| 后端 `application.yml` | `name: Ai-Blog` → `name: AI-Mall` |

**风险**：低——`package-lock.json` 需要重新生成并跑一次 `npm ci` 验证不报错；Spring 应用名一般只影响日志/监控里显示的服务名，不影响功能。

**回滚**：直接 `git revert`。

### 🟧 Tier 2 — Docker / CI-CD / 部署路径对齐（需要协调一次发布窗口，但不动生产数据）

这一层的关键是**顺序**：先在 CI 里把"新名字"跑通、验证镜像能正常构建部署，再切生产流量，避免出现"新工作流找不到旧镜像"的中间态。

| 改动项 | 操作 | 依赖/前置条件 |
|---|---|---|
| Docker 容器名 | `docker-compose.yml`/`docker-compose.prod.yml` 里 `ai-blog-web-*` → `ai-mall-web-*` | 无 |
| CI 镜像名 | `build-push-acr.yml`/`deploy-ecs.yml` 里 `BACKEND_IMAGE_NAME`/`FRONTEND_IMAGE_NAME` 从 `ai-blog-backend`/`ai-blog-frontend` → `ai-mall-backend`/`ai-mall-frontend` | 需要确认阿里云 ACR 目标 namespace 下允许新建这两个 repository（如 namespace 也要改，如 `blog_private`→`mall_private`，需先在 ACR 控制台手动创建新命名空间并授权，这一步不能通过代码完成） |
| 历史遗留镜像命名清理 | `scripts/release.ps1`、`README.md`、`frontend/Dockerfile`、`frontend/Dockerfile.release`、`backend/Dockerfile` 里残留的 `lxf_ai/al-blog-rouyi-*` 引用统一清理/更新为当前实际使用的镜像地址 | 借这次机会把历史遗留的"第三套命名"也清掉 |
| CI concurrency 分组名 | `deploy-ecs.yml` 的 `ai-blog-production-deploy` → `ai-mall-production-deploy` | 无 |
| 生产部署路径 | `ECS_DEPLOY_PATH: /opt/ai-blog` → `/opt/ai-mall` | **需要在 ECS 服务器上手动创建新目录并迁移/重建 `.env`**（工作流每次部署会自动重写 `.env`，所以新目录首次部署时会自动生成，旧目录 `/opt/ai-blog` 可在确认新目录跑通后手动清理） |
| 业务环境变量前缀 `BLOG_OSS_*` | 如果连这一层都要改（比如改成 `MALL_OSS_*`），需要同步改：`docker-compose.prod.yml`、`deploy-ecs.yml`、后端 `blog.oss.*` 配置属性与 `OssProperties` 绑定、**GitHub Environment `production` 里的 Secrets/Variables**（`BLOG_OSS_KEY_ID`/`BLOG_OSS_KEY_SECRET`/`BLOG_OSS_BUCKET`/`BLOG_OSS_ENABLED`） | GitHub Secrets **无法重命名，只能新建+删旧**，且新建后需要你重新填入密钥值（我没有权限读取现有密钥原文，这一步必须由你在 GitHub 网页上手动操作） |

> **建议**：`BLOG_OSS_*` 这层前缀是否要改，取决于第 5 节 Tier 4 是否要连 `ruoyi-blog` 模块本身都改名。如果只改"项目对外的名字"而不动 `ruoyi-blog` 这个内容子模块，`blog.oss.*` 保持不变是合理的（OSS 配置本来就是给"博客文件存储"用的，语义上没问题）。

**风险**：中——涉及 CI 密钥/变量、ACR 命名空间需要人工在控制台操作、生产部署路径切换需要一次实际发布验证。
**回滚**：保留旧 workflow 文件一个版本作为 fallback；ECS 上旧目录 `/opt/ai-blog` 在确认新流程稳定前不要删除。

### 🟥 Tier 3 — GitHub 仓库改名（低风险但涉及协作方同步）

| 步骤 | 操作 |
|---|---|
| 1 | GitHub 仓库设置里执行 Rename repository：`ai-blog-web` → `ai-mall-web` |
| 2 | GitHub 会自动把旧 URL 重定向到新 URL，但**本地/协作者的 git remote 建议主动更新**：`git remote set-url origin https://github.com/eric-lxf/ai-mall-web.git` |
| 3 | 检查是否有 GitHub App / Webhook / 第三方集成绑定了"仓库名字符串"而不是仓库 ID（一般现代集成都按 ID，风险低，但建议改名后跑一次 CI 确认 workflow 正常触发） |
| 4 | 更新 `frontend/package.json` 的 `repository.url` 为新地址（见 Tier 1） |

**风险**：低。**回滚**：GitHub 仓库改名可以随时改回旧名字（旧的重定向在改名后仍然有效一段时间）。

### 🟪 Tier 4（默认不建议做）— 数据库改名

数据库改名是**唯一真正有风险的一步**，因为涉及生产环境已有数据。独立在第 5 节详细展开。

### ⬛ Tier 5（默认不做，除非你要"连模块名都彻底改"）— Java 包名 / Maven 模块名

即把 `backend/ruoyi-blog` 模块本身连同 Java 包 `com.ruoyi.blog.*` 一起改名（例如改成 `ruoyi-content` / `com.ruoyi.content.*`）。

**不建议现在做**，理由：
- 影响面是整个模块内**所有** `.java` 文件的 `package` 声明和 import 路径，以及 `ruoyi-admin`、`ruoyi-wechat` 里对 `ruoyi-blog` 的依赖声明（`pom.xml` 的 `artifactId`）
- 这是纯代码重构，和"项目对外叫什么名字"是两件独立的事——项目改名不需要连带把每个业务模块的内部代码命名都推翻
- 违反最小变更原则：这类改动应该是独立的技术债务清理任务，而不是绑在"改名"这个任务里一起做

**如果确实要做**，建议单独立项，按包名重构常规做法（IDE 批量 rename package + 全量编译验证 + 回归测试），且**不与本次改名计划的其他 Tier 混在同一个分支**。

---

## 5. 数据库改名详细方案（Tier 4，谨慎执行）

### 5.1 为什么这一步风险最高

`ai_blog` 数据库如果已经在生产运行、有真实用户数据，改名等价于一次数据迁移，任何疏漏都可能造成数据丢失或服务中断。

### 5.2 推荐方案：同实例内 `RENAME TABLE` 迁移（而非 dump/restore）

如果新库和旧库在**同一个 MySQL 实例**上，`RENAME TABLE` 是元数据级操作（不搬运实际数据文件，速度快、锁表时间短），优于 `mysqldump` 导出再导入：

```sql
-- 1. 创建新库（字符集/排序规则与旧库保持一致）
CREATE DATABASE IF NOT EXISTS ai_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 逐表迁移（可用脚本批量生成，示例）
RENAME TABLE ai_blog.sys_user TO ai_mall.sys_user;
RENAME TABLE ai_blog.blog_article TO ai_mall.blog_article;
-- ... 对 information_schema.tables 里 ai_blog 下的每一张表重复执行

-- 3. 校验：新库表数量与行数应与旧库迁移前完全一致
SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema = 'ai_mall';
```

可以写一个一次性脚本，通过 `information_schema.tables` 查出 `ai_blog` 下所有表名，拼接生成完整的 `RENAME TABLE` 语句列表，一次性执行，避免手写遗漏。

### 5.3 执行步骤（建议在低峰期 + 停服窗口内完成）

1. **发布前**：确认所有连接串配置改动已在 Tier 1/2 里准备好（`application-druid.yml`、`application-docker.yml`、`docker-compose.yml`、GitHub Environment 的 `MYSQL_DATABASE` 变量），但**先不上线**
2. **停止后端服务写入**（简单模式：临时停止 backend 容器，避免迁移过程中有新写入落在旧库）
3. 执行 `RENAME TABLE` 批量脚本，把 `ai_blog.*` 全部迁移到 `ai_mall.*`
4. 校验：对比迁移前记录的表清单+行数 与 迁移后 `ai_mall` 的表清单+行数，逐一核对一致
5. 更新配置为指向 `ai_mall`，重启 backend 服务
6. 冒烟测试：登录、文章列表、（电商上线后）下单等关键路径手动验证一遍
7. **观察期**（建议至少覆盖一个完整业务高峰周期）：保留旧的空库结构 `ai_blog`（此时已无表，仅剩空 schema）作为"最后一道保险"标记，确认无异常后再 `DROP DATABASE ai_blog`
8. 同步更新 `sql/*.sql` 里所有 `USE ai_blog;` → `USE ai_mall;`，保证以后新环境初始化时用的是新库名

### 5.4 回滚方案

如果第 3-6 步之间发现问题：
- 因为 `RENAME TABLE` 是可逆操作，直接反向执行 `RENAME TABLE ai_mall.xxx TO ai_blog.xxx` 即可回滚到旧库名
- 应用配置改回指向 `ai_blog`，重启服务
- 前提：回滚窗口内**没有任何新数据写入 `ai_mall`**——所以第 3-6 步操作期间必须保证只有一个数据库在被写入（这也是为什么建议短暂停服而不是"边迁移边双写"，双写一致性保障的复杂度远高于短暂停服的成本）

### 5.5 影响面清单（改库名要同步改的地方）

| 文件 | 改动 |
|---|---|
| `backend/ruoyi-admin/.../application-druid.yml` | JDBC URL 里的库名 |
| `backend/ruoyi-admin/.../application-docker.yml` | `${MYSQL_DATABASE:ai_blog}` 默认值 |
| `docker-compose.yml` | `MYSQL_DATABASE: ai_blog`（mysql + backend 两处） |
| GitHub Environment `production` 的 Variables | `MYSQL_DATABASE` 变量值 |
| `sql/*.sql`（22 个文件） | 全部 `USE ai_blog;` → `USE ai_mall;` |
| `sql/gen_wechat_schema.js`/`gen_wechat_encoding_fix.js` | 生成 SQL 时用的库名常量 |
| `README.md`/`AGENTS.md` | 文档里写明的库名 |

---

## 6. 推荐执行顺序（依赖关系）

```
Tier 0（文档/文案） ──┐
                      ├──> 合并到 master，验证前端/后端正常构建
Tier 1（包名/应用名）─┘
        │
        ▼
Tier 2（Docker/CI/部署路径）── 需要人工在 ACR 控制台建新 namespace/repo（如需要）
        │                      需要人工在 GitHub Environment 重建 Secrets（如改 BLOG_OSS_* 前缀）
        │                      需要一次实际生产发布验证新路径/新镜像跑通
        ▼
Tier 3（GitHub 仓库改名）── 可随时做，风险最低，但建议放在 Tier 2 验证通过之后再做，
                            避免改名期间 CI workflow 的 checkout 地址产生混淆
        │
        ▼
Tier 4（数据库改名）── 独立挑选停服窗口执行，前置条件：Tier 1/2 的配置改动已就位但未生效
        │
        ▼
Tier 5（Java 包名重构）── 默认不做，除非单独立项
```

---

## 7. 验收标准

- [ ] Tier 0：`grep -r "Ai-Blog-Web" .` 在文档/前端范围内无残留（Java 代码里的 `com.ruoyi.blog` 包名不算，属于 Tier 5 范围）
- [ ] Tier 1：`cd frontend && npm ci && npm run build:prod` 成功，产物无报错；后端应用启动日志中显示新的应用名
- [ ] Tier 2：CI workflow 用新镜像名成功推送到 ACR，且能在测试环境用新的 `docker-compose.prod.yml` 配置成功拉起服务并通过 `/captchaImage` 冒烟
- [ ] Tier 2：生产 ECS 上 `/opt/ai-mall` 目录部署成功，服务健康检查通过，旧 `/opt/ai-blog` 观察期后清理
- [ ] Tier 3：GitHub 仓库显示新名字，`git remote -v` 在本地和 CI 里均已更新，CI workflow 能正常触发
- [ ] Tier 4：数据库改名后，登录、核心业务读写路径手动验证通过；观察期无异常后清理旧库
- [ ] 全部 Tier 完成后，仓库内不存在指向旧仓库名/旧库名的死链接或过期文档

---

## 8. 本次不做（Non-goals）

| 事项 | 原因 |
|---|---|
| Java 包名 `com.ruoyi.blog.*` 重构 | 与"项目改名"是两件独立的事，属于 Tier 5，建议单独立项（见第 4 节说明） |
| 域名/备案信息变更 | 当前仓库未发现已绑定自定义域名（`nginx.conf` 仅 `server_name localhost;`），如未来有域名需要一并规划，需要另外单独确认 |
| 微信公众号/小程序后台的 AppID、商户号等外部平台配置改名 | 这些是微信开放平台侧的注册信息，与代码仓库改名无关，不在本计划范围 |
| 阿里云 OSS Bucket 改名 | Bucket 一旦创建域名会变化，涉及历史文件访问链接失效，建议除非明确需要，否则保留现有 Bucket，仅在需要时新建新 Bucket 用于新业务，不做存量迁移 |

---

## 9. 需要你确认的问题

1. 最终选定哪个候选名字（或提供你自己想到的名字）？
2. 是否要执行 Tier 4（数据库改名）？如果生产环境已有真实数据，建议明确一个可接受的停服窗口。
3. `BLOG_OSS_*` 环境变量前缀、`blog.oss.*` 配置属性是否也要改（取决于是否连带做 Tier 5）？
4. 阿里云 ACR 的 namespace `blog_private` 是否也要改成新名字（需要你或有权限的同事在 ACR 控制台操作）？

确认后我会按 Tier 拆分成独立分支逐步落地，Tier 0-3 可以在一个改名分支里一次性完成并测试，Tier 4 单独开分支并配合实际数据库操作窗口执行。
