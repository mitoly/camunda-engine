package com.example.workflow.vo;

import lombok.Data;

@Data
public class ResultVo {

    private String code;
    private String msg;
    private Object data;

    public ResultVo() {

    }

    public ResultVo(String code, Object data) {
        this.code = code;
        this.data = data;
    }

    public static ResultVo success(Object data) {
        return new ResultVo("200", data);
    }
}
