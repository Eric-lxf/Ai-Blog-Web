# NovaMall 重命名执行说明

**选定名称**：NovaMall  
**分支**：`cursor/rename-to-novamall-8d5f`  
**日期**：2026-07-17  

## 已完成的替换

| 层 | 旧值 | 新值 |
|---|---|---|
| 项目显示名 | Ai-Blog-Web / AI-Mall-Web | **NovaMall** |
| npm 包名 | ai-blog-web | **nova-mall-web** |
| Spring 应用名 | Ai-Blog | **NovaMall** |
| 数据库名 | ai_blog | **nova_mall** |
| Docker 容器名 | ai-blog-web-* | **nova-mall-web-*** |
| CI 镜像名 | ai-blog-backend/frontend | **nova-mall-backend/frontend** |
| ACR namespace | blog_private | **nova_mall** |
| 部署路径 | /opt/ai-blog | **/opt/nova-mall** |
| OSS 环境变量 | BLOG_OSS_* | **NOVAMALL_OSS_*** |
| OSS 配置前缀 | blog.oss | **novamall.oss** |
| 代码生成作者 | ai-blog | **nova-mall** |

## 刻意未改（业务模块内部命名）

- Maven 模块 `ruoyi-blog` 与 Java 包 `com.ruoyi.blog.*`
- 权限标识 `blog:*`、菜单文案「AI博客」
- 配置前缀 `blog.file` / `blog.notification`（属于博客业务域）
- Dockerfile 里引用的公共基础镜像路径 `.../lxf_ai/maven|node|nginx`（与项目产物无关）

## 本地使用注意

1. 数据库需用新库名初始化：`sql/00-init-db.sql` 会创建 `nova_mall`
2. 若本机曾跑过旧库 `ai_blog`，请重新建库或手动迁移，不要混用
3. ACR 需在阿里云控制台创建命名空间 `nova_mall` 后，CI 推送镜像才能成功
4. GitHub Environment Secrets 若启用 OSS，需把 `BLOG_OSS_*` 改建成 `NOVAMALL_OSS_*`
5. GitHub 仓库本体改名为 `NovaMall` 需在 GitHub Settings 手动完成（代码侧 `package.json` repository.url 已指向新名）
