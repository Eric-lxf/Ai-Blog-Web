SET NAMES utf8mb4;
USE nova_mall;

-- 已部署环境：修正 AI 博客菜单路由，避免与公开前台 /blog 冲突，并统一子菜单 path
UPDATE sys_menu SET path = 'blog-admin' WHERE menu_id = 2000;
UPDATE sys_menu SET path = 'article/edit' WHERE menu_id = 2002;
UPDATE sys_menu SET path = 'ai/write' WHERE menu_id = 2003;
UPDATE sys_menu SET path = 'ai/optimize' WHERE menu_id = 2004;

-- 菜单图标（须与 frontend/src/assets/icons/svg 下文件名一致）
UPDATE sys_menu SET icon = 'documentation' WHERE menu_id = 2000;
UPDATE sys_menu SET icon = 'chart' WHERE menu_id = 2001;
UPDATE sys_menu SET icon = 'edit' WHERE menu_id = 2002;
UPDATE sys_menu SET icon = 'star' WHERE menu_id = 2003;
UPDATE sys_menu SET icon = 'clipboard' WHERE menu_id = 2004;
UPDATE sys_menu SET icon = 'list' WHERE menu_id = 2005;
