package com.school.library.entity;

public enum LoanStatus {
    /** 在借 */
    ACTIVE,
    /** 已归还 */
    RETURNED,
    /** 逾期未还 */
    OVERDUE
}
