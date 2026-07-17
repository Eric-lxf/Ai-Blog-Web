-- 已有库：去掉界面可见的「若依」品牌文案与官网菜单（幂等）
USE ai_blog;

-- 侧栏「若依官网」外链
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4;

-- 部门 / 用户昵称示例数据
UPDATE sys_dept SET dept_name = '总公司', leader = '管理员', email = 'admin@example.com' WHERE dept_id = 100 AND dept_name = '若依科技';
UPDATE sys_dept SET leader = '管理员', email = 'admin@example.com' WHERE leader = '若依';
UPDATE sys_user SET nick_name = '管理员', email = 'admin@example.com' WHERE user_id = 1 AND nick_name = '若依';
UPDATE sys_user SET nick_name = '测试员', email = 'test@example.com' WHERE user_id = 2 AND nick_name = '若依';

-- 通知公告
UPDATE sys_notice SET notice_title = '温馨提醒：系统已就绪', notice_content = '欢迎使用 AI 博客管理后台'
WHERE notice_id = 1 AND notice_title LIKE '%若依%';
UPDATE sys_notice SET notice_title = '维护通知：请关注系统更新'
WHERE notice_id = 2 AND notice_title LIKE '%若依%';
UPDATE sys_notice SET notice_title = 'AI博客介绍',
  notice_content = '<p>AI博客管理后台，支持文章、评论、账单识别与多模型 AI 能力。</p>'
WHERE notice_id = 3 AND (notice_title LIKE '%若依%' OR notice_content LIKE '%ruoyi.vip%');
