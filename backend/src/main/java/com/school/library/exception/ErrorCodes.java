package com.school.library.exception;

import org.springframework.http.HttpStatus;

/** 常用错误码定义，与前端约定保持稳定 */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";
    public static final String OLD_PASSWORD_MISMATCH = "OLD_PASSWORD_MISMATCH";
    public static final String BORROW_LIMIT_EXCEEDED = "BORROW_LIMIT_EXCEEDED";
    public static final String COPY_NOT_AVAILABLE = "COPY_NOT_AVAILABLE";
    public static final String READER_RESTRICTED = "READER_RESTRICTED";
    public static final String RENEWAL_NOT_ALLOWED = "RENEWAL_NOT_ALLOWED";
    public static final String RESERVED_CANNOT_RENEW = "RESERVED_CANNOT_RENEW";
    public static final String LOAN_NOT_ACTIVE = "LOAN_NOT_ACTIVE";
    public static final String WITHDRAW_NOT_ALLOWED = "WITHDRAW_NOT_ALLOWED";
    public static final String ALREADY_OVERDUE = "ALREADY_OVERDUE";

    public static final HttpStatus STATUS_VALIDATION = HttpStatus.BAD_REQUEST;

    private ErrorCodes() {
    }
}
