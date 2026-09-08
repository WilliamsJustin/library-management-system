package com.school.library.entity;

public enum CopyStatus {
    /** 在库，可借 */
    IN_STOCK,
    /** 已借出 */
    BORROWED,
    /** 已下架 */
    WITHDRAWN
}
