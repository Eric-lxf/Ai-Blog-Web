SET NAMES utf8mb4;
USE nova_mall;

-- 后台「商城」目录 path 从 mall 改为 mall-admin，避免与 C 端公开路由 /mall 冲突。
-- 冲突表现：菜单进入后台商品/类目等页正常，浏览器刷新同一 URL 落到公开 /mall 布局并 404。
-- 对齐博客后台使用 blog-admin、公开前台使用 /blog 的约定。

UPDATE sys_menu
SET path = 'mall-admin',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '商城管理目录（path=mall-admin，避免与C端/mall冲突）'
WHERE menu_id = 3000
  AND path = 'mall';
