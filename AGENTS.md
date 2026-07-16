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

### 时区（重要）

业务统一使用 **Asia/Shanghai（东八区）**。宿主机若为 UTC，会出现库里存 `02:30`、页面显示偏差（例如再减 8 小时变成前一天 `18:30`）。

- 后端启动强制 `TimeZone.setDefault(Asia/Shanghai)`；Jackson `spring.jackson.time-zone: Asia/Shanghai`
- Docker：`TZ=Asia/Shanghai`，MySQL `--default-time-zone=+08:00`，JVM `-Duser.timezone=Asia/Shanghai`
- 非 Docker 本机 MySQL 请执行：`SET GLOBAL time_zone = '+08:00';`（或写入 my.cnf）
- JDBC URL 已含 `serverTimezone=Asia/Shanghai`，须与 MySQL 会话时区一致

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

- 推荐在管理后台 **AI博客 → AI模型配置** 中配置多个 Provider（OpenAI / Claude / DeepSeek 等）。
- 仍兼容环境变量 `DEEPSEEK_API_KEY`（`.env`）作为回退；未配置时非 AI 流程仍可用。
