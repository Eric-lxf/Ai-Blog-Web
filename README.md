# Ai-Blog-Web

RuoYi-Vue 3.9.2（完整系统管理）+ ai-blog 博客业务融合项目。

## 架构

前后端分离，**UI 只在仓库根目录 `frontend/`，`backend/` 为纯 Java 后端**（不含 RuoYi 自带的 Vue2 `ruoyi-ui`）。

- **backend**：RuoYi Maven 多模块 + `ruoyi-blog`（文章/AI/上传）
  - `ruoyi-admin`（启动入口）、`ruoyi-framework`、`ruoyi-system`、`ruoyi-common`、`ruoyi-quartz`、`ruoyi-generator`、`ruoyi-blog`
- **frontend**：RuoYi-Vue3（Vue3 + Element Plus）+ 博客创作中心 + 公开前台 `/blog`
- **数据**：MySQL `ai_blog` + Redis（Token）

## 本地开发

### 1. 初始化数据库

按顺序执行 `sql/` 下脚本（或使用 Docker 自动初始化）：

```bash
mysql -u root -p < sql/00-init-db.sql
mysql -u root -p < sql/ry_base.sql
mysql -u root -p < sql/quartz.sql
mysql -u root -p < sql/blog_schema.sql
mysql -u root -p < sql/blog_comment_schema.sql
mysql -u root -p < sql/blog_notification_schema.sql
mysql -u root -p < sql/blog_notification_menu_fix.sql
mysql -u root -p < sql/blog_comment_menu_route_fix.sql
mysql -u root -p < sql/blog_analytics_schema.sql
mysql -u root -p < sql/blog_menu_seed.sql
mysql -u root -p < sql/wechat_schema.sql
mysql -u root -p < sql/wechat_menu_route_fix.sql
```

修改 `backend/ruoyi-admin/src/main/resources/application-druid.yml` 中的数据库账号，库名建议 `ai_blog`。

### 2. 启动 Redis

RuoYi Token 依赖 Redis，本地需运行 Redis（默认 `localhost:6379`）。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run -pl ruoyi-admin
```

默认账号：`admin` / `admin123`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

## Docker 构建（阿里云个人镜像仓库）

**构建上下文必须是仓库根目录 `Ai-Blog-Web/`**，不能是 `frontend/` 子目录。

```bash
# 1. 登录私有仓库（否则会 unauthorized）
docker login crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com

# 2. 在仓库根目录构建
cd Ai-Blog-Web
docker build -f frontend/Dockerfile -t crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/al-blog-rouyi-web:latest .
docker build -f backend/Dockerfile -t crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/al-blog-rouyi-server:latest .

# 3. 推送（构建成功后再 push）
docker push crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/al-blog-rouyi-web:latest
docker push crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/al-blog-rouyi-server:latest
```

常见错误：

| 现象 | 原因 | 处理 |
|------|------|------|
| `npm error signal SIGKILL`（多在 `rendering chunks`） | 构建峰值内存超过宿主机可用内存（2C4G 常见） | 见下方 **「2C4G 前端构建」**；或加大内存 / 加 swap / 在 CI 机构建镜像 |

### 2C4G 云主机前端构建（内存优化）

Vite 在 Docker 构建容器内峰值约 **1.3GB+ 堆内存**，2C4G 机器容器内直接 `npm run build:prod` 极易 OOM。

**2C4G 不能靠调大 Docker 内存解决**：物理只有 4GB，Docker 构建容器最多用剩余内存；vite 峰值约 1.3GB+，容器内构建必 OOM。

**正确方案**：宿主机/CI 先 `vite build` 生成 `frontend/dist`，`frontend/Dockerfile` 只打 nginx 包（几秒完成）。

```bash
# 推荐：一键脚本（含宿主机 build dist）
bash scripts/build-frontend-image.sh crpi-xxx/lxf_ai/al-blog-rouyi-web:latest
docker login crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com
docker push crpi-xxx/lxf_ai/al-blog-rouyi-web:latest
```

**CI 必须两步**（不要只 `docker build` 旧版全量 Dockerfile）：

```bash
cd frontend && npm ci && npm run build:prod
cd .. && docker build -f frontend/Dockerfile -t <镜像名> .
```

**加 swap 有帮助**（给宿主机 build 用，不是给 docker 内 vite）：

```bash
bash scripts/setup-swap.sh 2G
```

**8G+ 机器**才可在 Docker 内完整 vite 构建：

```bash
docker build -f frontend/Dockerfile.full --build-arg NODE_HEAP_MB=2048 -t ai-blog-web:local .
```

| 优化项 | 作用 |
|--------|------|
| `reportCompressedSize: false` | 跳过构建期 gzip 体积统计，降低 rendering chunks 内存 |
| `manualChunks` 拆分 echarts/mermaid | 降低单 chunk 峰值 |
| `maxParallelFileOps: 2` | 适配 2 核，减少并行占用 |
| 关闭 `vite-plugin-compression` | gzip 由 nginx 负责 |
| `.dockerignore` | 缩小构建上下文，加快 COPY |
| `unauthorized: authentication required` | 未登录阿里云镜像仓库 | 先执行 `docker login crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com` |
| `tag does not exist` | 上一步 build 失败，本地没有镜像 | 先让 `docker build` 成功再 `docker push` |
| `COPY nginx.conf` not found | 构建上下文或路径错误 | 在仓库根目录构建，使用 `COPY frontend/nginx.conf` |

## Docker 一键部署

```bash
cp .env.example .env
# 编辑 DEEPSEEK_API_KEY
docker compose --env-file .env up -d --build
# 或（云主机 / CI 推荐，失败会 exit 1 并打印后端日志）
chmod +x deploy.sh && ./deploy.sh
```

### 云部署报错 `ExitCode expect in [0] but is 1`

多为远程执行命令失败（阿里云 ECS 云助手、流水线等）。按顺序排查：

| 步骤 | 命令 | 说明 |
|------|------|------|
| 1 | `docker compose build backend 2>&1 \| tail -50` | Maven 构建失败：检查网络、是否上传完整 `backend/` |
| 2 | `docker compose build frontend 2>&1 \| tail -50` | 前端构建失败：需存在 `frontend/package-lock.json` |
| 3 | `docker compose logs mysql` | 初始化 SQL 失败：删卷重来 `docker compose down -v` |
| 4 | `docker compose logs backend` | 连不上 MySQL/Redis：确认 mysql、redis 已 healthy |
| 5 | `docker compose ps` | backend 未 healthy：首次启动约 1–2 分钟，已放宽 `start_period` |

本地仅编译（不启 Docker）：

```bash
cd backend && mvn -B -DskipTests package -pl ruoyi-admin -am
cd frontend && npm ci && npm run build:prod
```

| 地址 | 说明 |
|------|------|
| http://localhost | 前端（Nginx 反代 `/prod-api` → 后端） |
| http://localhost:8080 | 后端 API |
| http://localhost/blog | 博客公开前台 |
| 登录后侧边栏 | 系统管理 + AI博客 |

## 权限说明

- 用户/角色/菜单在 RuoYi **系统管理** 中配置
- 博客权限标识：`blog:article:*`、`blog:ai:*` 等（见 `sql/blog_menu_seed.sql`）
- 测试角色 `blog_editor`（role_id=3）：可管文章，不可「博客智写」

## 目录

```
Ai-Blog-Web/
├── backend/              # 纯后端（Java / Maven）
│   ├── ruoyi-admin/      # Spring Boot 启动模块
│   ├── ruoyi-framework/
│   ├── ruoyi-system/
│   ├── ruoyi-common/
│   ├── ruoyi-quartz/
│   ├── ruoyi-generator/
│   └── ruoyi-blog/       # 博客业务扩展
├── frontend/             # 唯一前端（RuoYi-Vue3 + 博客页面）
├── sql/                  # 数据库初始化
├── docker/               # Dockerfile 与 Nginx
└── docker-compose.yml
```

> **说明**：原 RuoYi-Vue 仓库内的 `ruoyi-ui`（Vue2）已移除，避免与 `frontend/` 混淆；系统管理、监控、代码生成等页面均在 `frontend/src/views/` 中维护。
