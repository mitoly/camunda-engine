package com.example.workflow.controller;

import com.example.workflow.dto.ProcessDto;
import com.example.workflow.vo.ResultVo;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/process")
public class CamundaController {

    @Autowired
    private IdentityService identityService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private HistoryService historyService;

    /**
     * 发起流程
     */
    @PostMapping("/start-process")
    public ResultVo startProcess(@RequestBody ProcessDto processDto) {
        String processDefinitionKey = processDto.getProcessDefinitionKey();
        String businessKey = processDto.getBusinessKey();
        identityService.setAuthenticatedUserId(processDto.getAssignee());
        //通过Key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, processDto.getVariableMap());
        BeanUtils.copyProperties(processInstance, processDto);
        return ResultVo.success(processDto);
    }

    /**
     * 删除流程
     */
    @PostMapping("/remove-process")
    public void removeProcess(@RequestBody ProcessDto processDto) {
        String processInstanceId = processDto.getProcessInstanceId();
        String comment = processDto.getComment();
        runtimeService.deleteProcessInstance(processInstanceId, comment);
    }

    /**
     * 查询流程状态
     */
    @PostMapping("/search-process-status/{processInstanceId}")
    public void searchProcessStatus(@PathVariable("processInstanceId") String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        System.out.println(processInstance);
    }

    /**
     * 查询代办任务
     */
    @PostMapping("/search-task/{assignee}")
    public ResultVo searchTask(@PathVariable("assignee") String assignee) {
        List<Task> taskList = taskService.createTaskQuery().taskAssignee(assignee).list();
        return ResultVo.success(taskList);
    }

    /**
     * 查询历史任务
     */
    @PostMapping("/search-history/{assignee}")
    public ResultVo searchHistory(@PathVariable("assignee") String assignee) {
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().taskAssignee(assignee).finished().list();
        return ResultVo.success(historicTaskInstances);
    }

    /**
     * 查询审批意见
     */
    @PostMapping("/search-task-comment/{taskId}")
    public ResultVo searchTaskComment(@PathVariable("taskId") String taskId) {
        List<Comment> taskComments = taskService.getTaskComments(taskId);
        return ResultVo.success(taskComments);
    }

    /**
     * 查询审批意见
     */
    @PostMapping("/search-process-comment/{processInstanceId}")
    public ResultVo searchProcessComment(@PathVariable("processInstanceId") String processInstanceId) {
        List<Comment> taskComments = taskService.getProcessInstanceComments(processInstanceId);
        return ResultVo.success(taskComments);
    }

    /**
     * 审批任务
     */
    @PostMapping("/complete-task")
    public void completeTask(@RequestBody ProcessDto processDto) {
        String assignee = processDto.getAssignee();
        String taskId = processDto.getTaskId();
        String processInstanceId = processDto.getProcessInstanceId();
        String comment = processDto.getComment();

        //添加审批人
        identityService.setAuthenticatedUserId(assignee);
        //添加审批意见，可在Act_Hi_Comment里的message查询到
        //三个参数分别为待办任务ID,流程实例ID,审批意见
        taskService.createComment(taskId, processInstanceId, comment);
        //任务完成  也就是审批通过
        taskService.complete(taskId, processDto.getVariableMap());
    }

    /**
     * 驳回任务
     */
    @PostMapping("/reject-task")
    public void rejectTask(@RequestBody ProcessDto processDto) {
        String assignee = processDto.getAssignee();
        String processInstanceId = processDto.getProcessInstanceId();
        String taskId = processDto.getTaskId();
        String activityId = processDto.getActivityId();
        String comment = processDto.getComment();
        //添加审批人
        identityService.setAuthenticatedUserId(assignee);
        taskService.createComment(taskId, processInstanceId, comment);
        //获取当前环节实例
        ActivityInstance activity = runtimeService.getActivityInstance(processInstanceId);
        runtimeService.createProcessInstanceModification(processInstanceId)
                //关闭相关任务
                .cancelActivityInstance(activity.getId())
                .setAnnotation(comment)
                //启动目标活动节点
                .startBeforeActivity(activityId)
                .execute();
    }

}
