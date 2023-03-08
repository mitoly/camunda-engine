package com.example.workflow.vo;

import lombok.Data;

@Data
public class ResultVo {

    private String status;
    private String message;
    private Object data;

    public ResultVo() {

    }

    public ResultVo(String status, Object data) {
        this.status = status;
        this.data = data;
    }

    public ResultVo(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static ResultVo success() {
        return new ResultVo("200", null);
    }

    public static ResultVo success(Object data) {
        return new ResultVo("200", data);
    }

    public static ResultVo failure(String message, Object data) {
        return new ResultVo("500", message, data);
    }

    public static ResultVo failure(String status, String message) {
        return new ResultVo(status, message, null);
    }
}
