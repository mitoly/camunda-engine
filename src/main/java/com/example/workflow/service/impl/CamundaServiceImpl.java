package com.example.workflow.service.impl;

import com.example.workflow.dto.ProcessDto;
import com.example.workflow.entity.ActHiComment;
import com.example.workflow.mapper.ActHiCommentMapper;
import com.example.workflow.service.ICamundaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CamundaServiceImpl implements ICamundaService {

    @Autowired
    private ActHiCommentMapper actHiCommentMapper;

    public void addComment(ProcessDto processDto, String type, String action) {
        ActHiComment actHiComment = new ActHiComment();
        actHiComment.setType(type);
        actHiComment.setTime(new Date());
        actHiComment.setUserId(StringUtils.isNotBlank(processDto.getCommentUser()) ? processDto.getCommentUser() : processDto.getAssignee());
        actHiComment.setTaskId(processDto.getExecutionId());
        actHiComment.setRootProcInstId(processDto.getProcessInstanceId());
        actHiComment.setProcInstId(processDto.getProcessInstanceId());
        actHiComment.setAction(action);
        actHiComment.setMessage(processDto.getComment());
        actHiComment.setFullMsg(processDto.getComment());
        actHiCommentMapper.insert(actHiComment);
    }
}
