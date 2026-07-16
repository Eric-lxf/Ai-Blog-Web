-- 操作日志全覆盖：扩展操作类型字典 + 90天清理定时任务
-- 适用于已有库增量升级；全新安装已包含在 ry_base.sql

-- 扩展 sys_oper_type：同步 / 连通测试 / 发布推送 / AI调用
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 30, 10, '同步', '10', 'sys_oper_type', '', 'info', 'N', '0', 'admin', sysdate(), '同步操作'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_oper_type' AND dict_value = '10');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 31, 11, '连通测试', '11', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', sysdate(), '连通性测试'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_oper_type' AND dict_value = '11');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 32, 12, '发布推送', '12', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', sysdate(), '发布推送操作'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_oper_type' AND dict_value = '12');

INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 33, 13, 'AI调用', '13', 'sys_oper_type', '', 'success', 'N', '0', 'admin', sysdate(), 'AI调用操作'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'sys_oper_type' AND dict_value = '13');

-- 每日凌晨 2 点清理超过 90 天的操作日志（status=0 正常启用）
INSERT INTO sys_job (job_id, job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT 4, '操作日志清理', 'SYSTEM', 'operLogTask.cleanExpired(90)', '0 0 2 * * ?', '3', '1', '0', 'admin', sysdate(), '保留最近90天操作日志'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'operLogTask.cleanExpired(90)');
