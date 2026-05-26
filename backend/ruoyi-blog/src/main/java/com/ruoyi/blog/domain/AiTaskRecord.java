package com.ruoyi.blog.domain;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("ai_task_record")
public class AiTaskRecord
{

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private Long targetArticleId;
    private String promptPayload;
    private String intermediateData;
    private String resultContent;
    /** 0-排队, 1-生成中, 2-成功, 3-失败 */
    private Integer status;
    private String errorMessage;
    private Integer tokensUsed;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
