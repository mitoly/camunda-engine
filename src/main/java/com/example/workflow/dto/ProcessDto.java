package com.example.workflow.dto;

import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class ProcessDto {

    /**当前审批人*/
    private String assignee;

    /**流程定义Key*/
    private String processDefinitionKey;

    /**流程定义Id*/
    private String processDefinitionId;

    /**流程实例Id*/
    private String processInstanceId;

    /**租户ID*/
    private String tenantId;

    /**流程单号，在流程发起时由业务提供*/
    private String businessKey;

    /**任务Id*/
    private String taskId;

    /**节点Id*/
    private String activityId;

    /**审批时填写的审批意见*/
    private String comment;

    /**审批意见填写人，当commentUser为空时，取assignee*/
    private String commentUser;

    /**流程环境变量*/
    private Map<String, Object> variableMap;

    private Set<String> queryProcessInstanceIdSet;
    private Set<String> queryProcessBusinessKeySet;

    /**消息事件名*/
    private String messageName;

    /**事件执行ID*/
    private String executionId;

    /**任务定义key*/
    private String taskDefinitionKey;

}
