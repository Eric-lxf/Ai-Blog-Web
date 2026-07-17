SET NAMES utf8mb4;
USE nova_mall;

-- 每 5 分钟取消超过待支付有效期的商城订单（status=0 正常启用）
INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
SELECT '商城订单超时取消', 'MALL', 'mallOrderTask.cancelExpiredOrders()', '0 */5 * * * ?', '3', '1', '0', 'admin', sysdate(), '取消过期未支付订单并恢复库存'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'mallOrderTask.cancelExpiredOrders()');
