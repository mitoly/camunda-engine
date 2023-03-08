package com.example.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName("ACT_HI_COMMENT")
@Data
public class ActHiComment {

    @TableId("ID_")
    private String id;

    @TableField("TYPE_")
    private String type;

    @TableField("TIME_")
    private Date time;

    @TableField("USER_ID_")
    private String userId;

    @TableField("TASK_ID_")
    private String taskId;

    @TableField("ROOT_PROC_INST_ID_")
    private String rootProcInstId;

    @TableField("PROC_INST_ID_")
    private String procInstId;

    @TableField("ACTION_")
    private String action;

    @TableField("MESSAGE_")
    private String message;

    @TableField("FULL_MSG_")
    private String fullMsg;

    @TableField("TENANT_ID_")
    private String tenantId;

    @TableField("REMOVAL_TIME_")
    private Date removalTime;

}
