package com.ruoyi.framework.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.ISysOperLogService;

/**
 * 操作日志定时清理任务（供 Quartz 调用：operLogTask.cleanExpired(90)）
 */
@Component("operLogTask")
public class OperLogTask
{
    private static final Logger log = LoggerFactory.getLogger(OperLogTask.class);

    private static final int DEFAULT_RETENTION_DAYS = 90;

    @Autowired
    private ISysOperLogService operLogService;

    /**
     * 清理超过默认保留天数的操作日志
     */
    public void cleanExpired()
    {
        cleanExpired(DEFAULT_RETENTION_DAYS);
    }

    /**
     * 清理超过指定天数的操作日志
     *
     * @param days 保留天数
     */
    public void cleanExpired(Integer days)
    {
        int retentionDays = days == null || days < 1 ? DEFAULT_RETENTION_DAYS : days;
        int deleted = operLogService.deleteOperLogByDays(retentionDays);
        log.info("操作日志清理完成，保留最近{}天，删除{}条", retentionDays, deleted);
    }
}
