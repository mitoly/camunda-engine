package com.example.workflow.service;

import com.example.workflow.dto.ProcessDto;

public interface ICamundaService {

    /**
     * 非任务审批中添加审批意见
     * 缺点是无法找到对应的节点。
     * @param processDto
     * @param type 类型区分
     * @param action 添加情况区分
     */
    void addComment(ProcessDto processDto, String type, String action);

}
