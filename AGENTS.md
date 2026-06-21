# AGENTS.md

## Cursor Cloud specific instructions

### 产品概览

**Ai-Blog-Web**：RuoYi-Vue 3.9.2 管理后台 + `ruoyi-blog`（文章/评论/上传/AI）。唯一前端在 `frontend/`；后端在 `backend/`（`ruoyi-admin` 为启动模块）。

### 依赖服务（本地开发）

| 服务 | 用途 | 建议启动方式 |
|------|------|----------------|
| **MySQL 8** | 库 `ai_blog`，SQL 在 `sql/` | `docker compose --env-file .env up mysql -d`（见下方 `.env` 密码约定） |
| **Redis** | Token / 验证码 | 本机 `redis-server`，或 `docker compose up redis -d`（勿与 6379 端口冲突） |
| **Backend** | `:8080` | 见「后端」 |
| **Frontend** | Vite 开发服 | 见「前端」 |

本地 `application-druid.yml` 默认 **root / password**。若用 Compose 起 MySQL，请在 `.env` 中设置 `MYSQL_ROOT_PASSWORD=password`（可复制 `.env.example` 后改此项），与 JDBC 配置一致。

### 快照环境实况（Cursor Cloud，重要）

当前 VM 快照**未安装 Docker**；MySQL 8 与 Redis 改为 **apt 安装**，直接用 service 启动（无 systemd，需 `sudo`）：

- 启动依赖：`sudo service mysql start` 与 `sudo service redis-server start`。
- 连接 MySQL 用 **TCP**：`mysql -uroot -ppassword -h 127.0.0.1`（非 root 用户无法走 unix socket；`root@localhost` 密码即 `password`，与 JDBC 一致）。
- 库 `ai_blog` **已初始化**（含 schema 与 `admin` 用户），正常无需重跑 `sql/`。若需重载，请用 `mysql --force`：`sql/blog_notification_schema.sql` 第 4 行 `ALTER TABLE blog_article ADD COLUMN author_user_id ...` 非幂等，会与 `blog_schema.sql` 已含的同名列冲突报 `Duplicate column`（无害，可忽略）。
- 更新脚本只跑 `cd frontend && npm ci`；后端 Maven 依赖随构建命令拉取并缓存在 `~/.m2`（`mvn dependency:go-offline` 因跨模块依赖会失败，勿用）。
- 登录验证码：默认 `sys.account.captchaEnabled=true`（数学验证码，自动化读图困难）。自动化登录/接口测试可临时关闭：`UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'` 后**重启后端**（配置有缓存），测试完记得改回 `true`。
- 前端生产构建用 `npm run build:prod`（16G 内存默认堆足够）；`build:prod:lowmem`（1280MB 堆）在本机会 OOM，勿用。

### 后端

- 工具：**JDK 17+**、**Maven 3.8+**（仓库根无父 POM，在 `backend/` 下执行 Maven）。
- 首次或改代码后构建：`cd backend && mvn -B -DskipTests package -pl ruoyi-admin -am`
- 启动 JAR（推荐，避免 `spring-boot:run -pl ruoyi-admin` 缺兄弟模块）：  
  `sudo mkdir -p /home/ruoyi/logs && java -jar backend/ruoyi-admin/target/ruoyi-admin.jar`（工作目录为 `backend/` 时用相对路径 `ruoyi-admin/target/...`）
- 或用：`cd backend && mvn spring-boot:run -pl ruoyi-admin -am`（必须带 **`-am`**）
- 默认账号：`admin` / `admin123`
- 冒烟：`curl -s http://localhost:8080/captchaImage`

### 前端

- 工具：**Node 20+**（`frontend/package-lock.json` → `npm ci`）
- 开发：`cd frontend && npm run dev`  
  默认 `vite.config.js` 监听 **80**，非 root 环境请：`npm run dev -- --port 5173 --host 0.0.0.0`
- 代理：`/dev-api` → `http://localhost:8080`
- 生产构建：`npm run build:prod`

### Docker（Cloud VM）

- 守护进程可能需手动启动：`sudo dockerd`（存储驱动 `fuse-overlayfs` 见环境镜像说明）
- 无 docker 组时命令前加 **`sudo`**
- 仅基础设施：`docker compose --env-file .env up mysql redis -d`  
  全栈：`docker compose --env-file .env up -d --build`（见根目录 `README.md`）

### 公开地址（本地 dev 示例）

| URL | 说明 |
|-----|------|
| http://localhost:5173 | Vite（5173 示例端口） |
| http://localhost:5173/blog | 博客前台 |
| http://localhost:8080 | API |
| http://localhost:8080/swagger-ui.html | Swagger |

### 测试与 Lint

- 仓库内**无**统一 JUnit / Vitest / ESLint CI；验证以 **Maven 编译**、**前端 build**、**API/浏览器冒烟** 为主。
- 非主分支可参考团队约定在 `backend/` 执行 `mvn verify`（若后续补充测试再对齐）。

### AI 功能

- 需配置 `DEEPSEEK_API_KEY`（`.env` 或环境变量）；未配置时非 AI 流程仍可用。
