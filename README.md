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
mysql -u root -p < sql/blog_menu_seed.sql
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

## Docker 一键部署

```bash
cp .env.example .env
# 编辑 DEEPSEEK_API_KEY
docker compose --env-file .env up -d --build
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
