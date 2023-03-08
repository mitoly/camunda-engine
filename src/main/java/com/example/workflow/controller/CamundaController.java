package com.example.workflow.controller;

import com.example.workflow.dto.ProcessDto;
import com.example.workflow.service.ICamundaService;
import com.example.workflow.vo.CommentVo;
import com.example.workflow.vo.ResultVo;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Comment;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/***
 * 根据API进行满足业务的简单封装
 * 如不用，也可使用Camunda标准RestApi
 * RestApi调试地址，在项目Pom中打开camunda-engine-rest-openapi注释，引入依赖。在postman中导入解压后的json文件
 */
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
    @Autowired
    private ExternalTaskService externalTaskService;

    @Autowired
    private ICamundaService camundaService;

    /**
     * 发起流程
     */
    @PostMapping("/start-process")
    public ResultVo startProcess(@RequestBody ProcessDto processDto) {
        String processDefinitionKey = processDto.getProcessDefinitionKey();
        String businessKey = processDto.getBusinessKey();
        if (StringUtils.isBlank(processDto.getTenantId())) {
            identityService.setAuthenticatedUserId(processDto.getAssignee());
        } else {
            // 多租户
            identityService.setAuthentication(processDto.getAssignee(), null, Collections.singletonList(processDto.getTenantId()));
        }
        //通过Key
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, processDto.getVariableMap());
        // 如果存在备注。添加备注
        if (StringUtils.isNotBlank(processDto.getComment())) {
            processDto.setProcessInstanceId(processInstance.getProcessInstanceId());
            camundaService.addComment(processDto, "startProcess", "AddComment");
        }
        BeanUtils.copyProperties(processInstance, processDto);
        return ResultVo.success(processDto);
    }

    /**
     * 删除流程
     */
    @PostMapping("/remove-process")
    public ResultVo removeProcess(@RequestBody ProcessDto processDto) {
        String processInstanceId = processDto.getProcessInstanceId();
        String comment = processDto.getComment();
        // 为了防止并发量高时流程无法删除。如果传递实例ID先暂停该实例，再进行删除
        if (StringUtils.isNoneBlank(processInstanceId)) {
            runtimeService.suspendProcessInstanceById(processInstanceId);
        }
        runtimeService.deleteProcessInstance(processInstanceId, comment);
        return ResultVo.success();
    }

    /**
     * 查询流程状态
     * 如果为null，则流程已经完结
     */
    @PostMapping("/search-process-status/{processInstanceId}")
    public ResultVo searchProcessStatus(@PathVariable("processInstanceId") String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        return ResultVo.success(processInstance);
    }

    /**
     * 查询代办任务
     */
    @PostMapping("/search-task/{assignee}")
    public ResultVo searchTask(@PathVariable("assignee") String assignee) {
        List<Task> taskList = taskService.createTaskQuery().taskAssignee(assignee).list();
        return ResultVo.success(taskList);
    }
    @PostMapping("/search-task-by-teantid/{tenantId}")
    public ResultVo searchTaskByTenantId(@PathVariable("tenantId") String tenantId) {
        List<Task> taskList = taskService.createTaskQuery().tenantIdIn(tenantId).list();
        return ResultVo.success(taskList);
    }
    @PostMapping("/search-task/{tenantId}/{assignee}")
    public ResultVo searchTask(@PathVariable("tenantId") String tenantId, @PathVariable("assignee") String assignee) {
        List<Task> taskList = taskService.createTaskQuery().tenantIdIn(tenantId).taskAssignee(assignee).list();
        return ResultVo.success(taskList);
    }
    @PostMapping("/search-task/{tenantId}/{assignee}/{firstResult}/{maxResults}")
    public ResultVo searchTask(@PathVariable("tenantId") String tenantId, @PathVariable("assignee") String assignee, @PathVariable("firstResult") Integer firstResult, @PathVariable("maxResults") Integer maxResults) {
        List<Task> taskList = taskService.createTaskQuery().tenantIdIn(tenantId).taskAssignee(assignee).listPage(firstResult, maxResults);
        return ResultVo.success(taskList);
    }
    @PostMapping("/search-task")
    public ResultVo searchTask(@RequestBody() ProcessDto processDto) {
        if (StringUtils.isBlank(processDto.getTenantId())) {
            throw new RuntimeException("TenantId 不能为空");
        }
        TaskQuery taskQuery = taskService.createTaskQuery();
        taskQuery.tenantIdIn(processDto.getTenantId());
        if (StringUtils.isNotBlank(processDto.getProcessDefinitionKey())) {
            taskQuery.processDefinitionKey(processDto.getProcessDefinitionKey());
        }
        if (StringUtils.isNotBlank(processDto.getProcessDefinitionId())) {
            taskQuery.processDefinitionId(processDto.getProcessDefinitionId());
        }
        if (StringUtils.isNotBlank(processDto.getProcessInstanceId())) {
            taskQuery.processInstanceId(processDto.getProcessInstanceId());
        }
        if (StringUtils.isNotBlank(processDto.getBusinessKey())) {
            taskQuery.processInstanceBusinessKey(processDto.getBusinessKey());
        }
        if (StringUtils.isNotBlank(processDto.getAssignee())) {
            taskQuery.taskAssignee(processDto.getAssignee());
        }
        if (StringUtils.isNotBlank(processDto.getTaskDefinitionKey())) {
            taskQuery.taskDefinitionKey(processDto.getTaskDefinitionKey());
        }
        List<Task> taskList = taskQuery.list();
        return ResultVo.success(taskList);
    }
    @PostMapping("/search-tasks-by-instance-ids")
    public ResultVo searchTasksByInstanceIds(@RequestBody() ProcessDto processDto) {
        if (null == processDto.getQueryProcessInstanceIdSet() || processDto.getQueryProcessInstanceIdSet().isEmpty()) {
            throw new RuntimeException("QueryProcessInstanceIdSet 不能为空");
        }
        List<Task> taskList = taskService.createTaskQuery()
                .tenantIdIn(processDto.getTenantId())
                .processInstanceIdIn(processDto.getQueryProcessInstanceIdSet().toArray(new String[processDto.getQueryProcessInstanceIdSet().size()]))
                .list();
        return ResultVo.success(taskList);
    }

    /**
     * 根据任务定义key获取指定的任务，不指定具体的代办人
     * @param processDto
     * @return
     */
    @PostMapping("/search-task-by-definition-key")
    public ResultVo searchTaskByDefinitionKey(@RequestBody() ProcessDto processDto) {
        List<Task> taskList = taskService.createTaskQuery()
                .tenantIdIn(processDto.getTenantId())
                .processInstanceId(processDto.getProcessInstanceId())
                .processInstanceBusinessKey(processDto.getBusinessKey())
                .taskDefinitionKey(processDto.getTaskDefinitionKey()).list();
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
    @PostMapping("/search-history/{tenantId}/{assignee}")
    public ResultVo searchHistory(@PathVariable("tenantId") String tenantId, @PathVariable("assignee") String assignee) {
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().tenantIdIn(tenantId).taskAssignee(assignee).finished().list();
        return ResultVo.success(historicTaskInstances);
    }
    @PostMapping("/search-history/{tenantId}/{assignee}/{firstResult}/{maxResults}")
    public ResultVo searchHistory(@PathVariable("tenantId") String tenantId, @PathVariable("assignee") String assignee, @PathVariable("firstResult") Integer firstResult, @PathVariable("maxResults") Integer maxResults) {
        List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery().tenantIdIn(tenantId).taskAssignee(assignee).finished().listPage(firstResult, maxResults);
        return ResultVo.success(historicTaskInstances);
    }
    @PostMapping("/search-history-by-business-keys")
    public ResultVo searchHistoryByInstanceIds(@RequestBody() ProcessDto processDto) {
        if (null == processDto.getQueryProcessBusinessKeySet() || processDto.getQueryProcessBusinessKeySet().isEmpty()) {
            throw new RuntimeException("QueryProcessBusinessKeySet 不能为空");
        }
        List<HistoricTaskInstance> historicTaskInstanceList = historyService.createHistoricTaskInstanceQuery()
                .tenantIdIn(processDto.getTenantId())
                .processInstanceBusinessKeyIn(processDto.getQueryProcessBusinessKeySet().toArray(new String[processDto.getQueryProcessBusinessKeySet().size()]))
                .list();
        return ResultVo.success(historicTaskInstanceList);
    }

    /**
     * 查询流程实例状态
     */
    @PostMapping("/search-process-instance/{processInstanceId}")
    public ResultVo searchProcessInstance(@PathVariable("processInstanceId") String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        return ResultVo.success(processInstance);
    }

    /**
     * 查询流程实例状态，根据BusinessKey
     */
    @PostMapping("/search-process-instance-by-business-key/{tenantId}/{businessKey}")
    public ResultVo searchProcessInstanceByBusinessKey(@PathVariable("tenantId") String tenantId, @PathVariable("businessKey") String businessKey) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().tenantIdIn(tenantId).processInstanceBusinessKey(businessKey).singleResult();
        return ResultVo.success(processInstance);
    }

    /**
     * 查询流程实例状态，根据ProcessInstanceId
     */
    @PostMapping("/search-process-instance-by-instance-ids")
    public ResultVo searchProcessInstanceByInstancesIds(@RequestBody ProcessDto processDto) {
        if (null == processDto.getQueryProcessInstanceIdSet() || processDto.getQueryProcessInstanceIdSet().isEmpty()) {
            throw new RuntimeException("QueryProcessInstanceIdSet 不能为空");
        }
        List<ProcessInstance> processInstanceList = runtimeService.createProcessInstanceQuery().processInstanceIds(processDto.getQueryProcessInstanceIdSet()).list();
        return ResultVo.success(processInstanceList);
    }

    /**
     * 查询流程环节状态
     * @param processInstanceId
     * @return
     */
    @PostMapping("/search-activity-instance/{processInstanceId}")
    public ResultVo searchActivityInstance (@PathVariable("processInstanceId") String processInstanceId) {
        List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        return ResultVo.success(activityInstanceList);

//        for (HistoricActivityInstance instance : activityInstanceList) {
//            HistoricActivityInstanceEntity activityEntity = (HistoricActivityInstanceEntity) instance;
//            if("multiInstanceBody".equals(activityEntity.getActivityType())){
//
//                //多实例节点
//                int endIndex = activityEntity.getActivityId().indexOf("#");
//                String activityId = activityEntity.getActivityId().substring(0,endIndex);
//                if (4 == activityEntity.getActivityInstanceState()){
//                    //已完成
//                }else{
//                    //运行中
//                }
//            }
//        }
    }

    /**
     * 查询任务审批意见
     */
    @PostMapping("/search-task-comment/{taskId}")
    public ResultVo searchTaskComment(@PathVariable("taskId") String taskId) {
        List<Comment> taskComments = taskService.getTaskComments(taskId);
        return ResultVo.success(taskComments);
    }

    /**
     * 查询流程审批意见
     */
    @PostMapping("/search-process-comment/{processInstanceId}")
    public ResultVo searchProcessComment(@PathVariable("processInstanceId") String processInstanceId) {
        List<Comment> taskComments = taskService.getProcessInstanceComments(processInstanceId);
        List<HistoricActivityInstance> activityInstanceList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        Map<String, String> taskActivityNameMap = new HashMap<>();
        for (HistoricActivityInstance historicActivityInstance : activityInstanceList) {
            if (StringUtils.isBlank(historicActivityInstance.getTaskId())) {
                continue;
            }
            taskActivityNameMap.put(historicActivityInstance.getTaskId(), historicActivityInstance.getActivityName());
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<CommentVo> commentVoList = taskComments.stream().map(comment -> {
            CommentVo commentVo = new CommentVo();
            BeanUtils.copyProperties(comment, commentVo);
            commentVo.setTimeFormat(sdf.format(comment.getTime()));
            commentVo.setActivityName(taskActivityNameMap.get(comment.getTaskId()));
            return commentVo;
        }).collect(Collectors.toList());

        return ResultVo.success(commentVoList);
    }

    /**
     * 审批任务
     */
    @Transactional
    @PostMapping("/complete-task")
    public ResultVo completeTask(@RequestBody ProcessDto processDto) {
        String assignee = processDto.getAssignee();
        String taskId = processDto.getTaskId();
        String processInstanceId = processDto.getProcessInstanceId();
        String comment = processDto.getComment();

        //添加审批人
        if (StringUtils.isBlank(processDto.getTenantId())) {
            identityService.setAuthenticatedUserId(assignee);
        } else {
            // 多租户
            identityService.setAuthentication(assignee, null, Collections.singletonList(processDto.getTenantId()));
        }
        //添加审批意见，可在Act_Hi_Comment里的message查询到
        //三个参数分别为待办任务ID,流程实例ID,审批意见
        taskService.createComment(taskId, processInstanceId, comment);
        //任务完成  也就是审批通过
        taskService.complete(taskId, processDto.getVariableMap());
        return ResultVo.success();
    }

    /**
     * 驳回任务
     */
    @Transactional
    @PostMapping("/reject-task")
    public ResultVo rejectTask(@RequestBody ProcessDto processDto) {
        String assignee = processDto.getAssignee();
        String processInstanceId = processDto.getProcessInstanceId();
        String taskId = processDto.getTaskId();
        String activityId = processDto.getActivityId();
        String comment = processDto.getComment();
        //添加审批人
        if (StringUtils.isBlank(processDto.getTenantId())) {
            identityService.setAuthenticatedUserId(assignee);
        } else {
            // 多租户
            identityService.setAuthentication(assignee, null, Collections.singletonList(processDto.getTenantId()));
        }
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
        return ResultVo.success();
    }

    /**
     * 查询消息节点的执行ID，可以根据执行ID推动消息节点的执行
     */
    @PostMapping("/search-message-event-execution/{tenantId}/{processInstanceId}")
    public ResultVo searchEventExecution(@PathVariable("tenantId") String tenantId, @PathVariable("processInstanceId") String processInstanceId) {
        List<Execution> executions = runtimeService.createExecutionQuery().tenantIdIn(tenantId).processInstanceId(processInstanceId).list();
        return ResultVo.success(executions);
    }
    @PostMapping("/search-message-event-execution/{tenantId}/{processInstanceId}/{messageName}")
    public ResultVo searchMessageEventExecution(@PathVariable("tenantId") String tenantId, @PathVariable("processInstanceId") String processInstanceId, @PathVariable("messageName") String messageName) {
        List<Execution> executions = runtimeService.createExecutionQuery().tenantIdIn(tenantId).processInstanceId(processInstanceId).messageEventSubscriptionName(messageName).list();
        return ResultVo.success(executions);
    }

    /**
     * 根据消息名和执行ID触发消息任务
     * @return
     */
    @PostMapping("/execution-message-event")
    public ResultVo executionMessageEvent(@RequestBody ProcessDto processDto) {
        runtimeService.messageEventReceived(processDto.getMessageName(), processDto.getExecutionId(), processDto.getVariableMap());
        if (StringUtils.isNotBlank(processDto.getComment())
                && (StringUtils.isNotBlank(processDto.getCommentUser()) || StringUtils.isNotBlank(processDto.getAssignee()))
                && StringUtils.isNotBlank(processDto.getProcessInstanceId())) {
            camundaService.addComment(processDto, "messageEvent", "AddComment");
        }
        return ResultVo.success();
    }

    /**
     * 根据消息名和执行ID触发消息任务
     * @return
     */
    @PostMapping("/execution-message-event/{messageName}/{executionId}")
    public ResultVo executionMessageEvent(@PathVariable("messageName") String messageName, @PathVariable("executionId") String executionId) {
        runtimeService.messageEventReceived(messageName, executionId);
        return ResultVo.success();
    }

    /**
     * 根据executionId获取流程变量
     * @return
     */
    @PostMapping("/get-variable-by-execution-id/{tenantId}/{executionId}/{messageName}")
    public ResultVo getVariableByExecutionId(@PathVariable("tenantId") String tenantId, @PathVariable("executionId") String executionId, @PathVariable("messageName") String messageName) {
        return ResultVo.success(runtimeService.getVariables(executionId));
    }

    /**
     * 根据消息名获取流程变量
     * @return
     */
    @PostMapping("/get-variable-by-message-name/{tenantId}/{processInstanceId}/{messageName}")
    public ResultVo getVariableByMessageName(@PathVariable("tenantId") String tenantId, @PathVariable("processInstanceId") String processInstanceId, @PathVariable("messageName") String messageName) {
        List<Execution> executions = runtimeService.createExecutionQuery().tenantIdIn(tenantId).processInstanceId(processInstanceId).messageEventSubscriptionName(messageName).list();
        if (null != executions && executions.size() > 0) {
            return ResultVo.success(runtimeService.getVariables(executions.get(0).getId()));
        }
        return ResultVo.success();
    }

    /**
     * 转发任务，修改代办人
     */
    @PostMapping("/forward-task/{taskId}/{assignee}")
    public ResultVo forwardTask(@PathVariable("taskId") String taskId, @PathVariable("assignee") String assignee) {
        taskService.setAssignee(taskId, assignee);
        return ResultVo.success();
    }

    /**
     * 根据流程定义ID，获取流程图XML
     */
    @PostMapping("/get-process-model-xml/{processDefinitionId}")
    public ResultVo getProcessModelXml(@PathVariable("processDefinitionId") String processDefinitionId) {
        StringBuilder processModelXml = new StringBuilder();
        try {
            InputStream inputStream = repositoryService.getProcessModel(processDefinitionId);
            BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream)));
            while(br.ready()) {
                processModelXml.append(br.readLine());
            }
        } catch (Exception e) {
            throw new RuntimeException("流程定义ID错误，未找到对应流程图");
        }
        return ResultVo.success(processModelXml.toString());
    }

    /**
     * 根据流程定义ID，获取流程图XML
     */
    @PostMapping("/get-process-model-xml/{tenantId}/{processDefinitionKey}")
    public ResultVo getProcessModelXml(@PathVariable("tenantId") String tenantId, @PathVariable("processDefinitionKey") String processDefinitionKey) {
        // 根据KEY获取最新的流程版本
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().tenantIdIn(tenantId).processDefinitionKey(processDefinitionKey).latestVersion().singleResult();
        return this.getProcessModelXml(processDefinition.getId());
    }

    /**
     * 获取流程图高亮节点
     */
    @PostMapping("/get-process-high-light-node/{processInstanceId}/{assignee}")
    public ResultVo getProcessHighLightNode(@PathVariable("processInstanceId") String processInstanceId, @PathVariable("assignee") String assignee) {
        HistoricProcessInstance hisProIns = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        //System.out.println(hisProIns.getProcessDefinitionName()+" "+hisProIns.getProcessDefinitionKey());
        //===================已完成节点
        List<HistoricActivityInstance> finished = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        Set<String> highPoint = new HashSet<>();
        finished.forEach(t -> highPoint.add(t.getActivityId()));

        //=================待完成节点
        List<HistoricActivityInstance> unfinished = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).unfinished().list();
        Set<String> waitingToDo = new HashSet<>();
        unfinished.forEach(t -> waitingToDo.add(t.getActivityId()));

        //=================iDo 我执行过的
        Set<String> iDo = new HashSet<>(); //存放 高亮 我的办理节点
        List<HistoricTaskInstance> taskInstanceList = historyService.createHistoricTaskInstanceQuery().taskAssignee(assignee).finished().processInstanceId(processInstanceId).list();
        taskInstanceList.forEach(a -> iDo.add(a.getTaskDefinitionKey()));

        //===========高亮线
        Set<String> highLine = new HashSet<>(); //保存高亮的连线
        //获取流程定义的bpmn模型
        BpmnModelInstance bpmn = repositoryService.getBpmnModelInstance(hisProIns.getProcessDefinitionId());
        //已完成任务列表 可直接使用上面写过的
        List<HistoricActivityInstance> finishedList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
        int finishedNum = finishedList.size();
        //循环 已完成的节点
        for (int i = 0; i < finishedNum; i++) {
            HistoricActivityInstance finItem = finishedList.get(i);
            //根据 任务key 获取 bpmn元素
            ModelElementInstance domElement = bpmn.getModelElementById(finItem.getActivityId());
            if (null == domElement) {
                continue;
            }
            //转换成 flowNode流程节点 才能获取到 输出线 和输入线
            FlowNode act = (FlowNode)domElement;
            Collection<SequenceFlow> outgoing = act.getOutgoing();
            //循环当前节点的 向下分支
            outgoing.forEach(v->{
                String tarId = v.getTarget().getId();
                //已完成
                for (int j = 0; j < finishedNum; j++) {
                    //循环历史完成节点 和当前完成节点的向下分支比对
                    //如果当前完成任务 的结束时间 等于 下个任务的开始时间
                    HistoricActivityInstance setpFinish = finishedList.get(j);
                    String finxId = setpFinish.getActivityId();
                    if(tarId.equals(finxId)){
                        if(finItem.getEndTime().equals(setpFinish.getStartTime())){
                            highLine.add(v.getId());
                        }
                    }
                }
                //待完成
                for (int j = 0; j < unfinished.size(); j++) {
                    //循环待节点 和当前完成节点的向下分支比对
                    HistoricActivityInstance setpUnFinish = unfinished.get(j);
                    String finxId = setpUnFinish.getActivityId();
                    if(tarId.equals(finxId)){
                        if(finItem.getEndTime().equals(setpUnFinish.getStartTime())){
                            highLine.add(v.getId());
                        }
                    }
                }

            });
        }

        Map<String, Object> map = new HashMap<>();
        map.put("result", "ok");
        map.put("highPoint", highPoint);
        map.put("highLine", highLine);
        map.put("waitingToDo", waitingToDo);
        map.put("iDo", iDo);
        return ResultVo.success(map);
    }

}
