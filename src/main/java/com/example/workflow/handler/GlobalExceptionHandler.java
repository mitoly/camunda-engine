package com.example.workflow.handler;

import com.example.workflow.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.exception.NullValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理其他异常
     *
     * @param req
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public ResultVo exceptionHandler(HttpServletRequest req, Exception e) {
        log.error("未知异常！原因是:", e);
        return ResultVo.failure("500", e.getMessage());
    }

    @ExceptionHandler(value = NullValueException.class)
    public ResultVo nullValueExceptionHandler(HttpServletRequest req, Exception e) {
        log.error("未知异常！原因是:", e);
        return ResultVo.failure("500", e.getMessage());
    }

}
