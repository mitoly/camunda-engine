package com.example.workflow.vo;

import lombok.Data;

import java.util.Date;

@Data
public class CommentVo {

    private String activityName;

    private String id;

    private String type;

    private String userId;

    private Date time;
    private String timeFormat;

    private String taskId;

    private String processInstanceId;

    private String action;

    private String message;

    private String fullMessage;

    private String tenantId;

    private String rootProcessInstanceId;

    private Date removalTime;


}
