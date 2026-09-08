package com.school.library.exception;

/**
 * 业务异常：携带稳定的错误码，前端据此展示提示。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
