package com.school.library.entity;

public enum BookStatus {
    /** 在架，可正常流通 */
    ACTIVE,
    /** 已下架，不可新增借阅 */
    INACTIVE
}
