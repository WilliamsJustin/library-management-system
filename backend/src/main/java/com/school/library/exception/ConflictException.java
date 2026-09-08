package com.school.library.exception;

/** 数据冲突（唯一键重复、存在关联记录等）：HTTP 409 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCodes.CONFLICT, message);
    }
}
