SET NAMES utf8mb4;
USE nova_mall;

-- 后台「商城」目录 path 从 mall 改为 mall-admin，避免与 C 端公开路由 /mall 冲突。
UPDATE sys_menu
SET path = 'mall-admin',
    update_by = 'admin',
    update_time = sysdate(),
    remark = '商城管理目录（path=mall-admin，避免与C端/mall冲突）'
WHERE menu_id = 3000
  AND path = 'mall';
