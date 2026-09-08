package com.school.library.exception;

/** 资源不存在：HTTP 404 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCodes.NOT_FOUND, message);
    }
}
