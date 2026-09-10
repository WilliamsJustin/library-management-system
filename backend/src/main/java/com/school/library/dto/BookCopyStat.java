package com.school.library.dto;

import lombok.Data;

/**
 * 图书副本数量统计（按 book_id 分组）。
 * 用于图书列表一次性填充「总副本数 / 可借副本数」，避免逐本查询造成 N+1。
 */
@Data
public class BookCopyStat {

    private Long bookId;

    /** 总副本数 */
    private Long totalCopies;

    /** 在架（可借）副本数 */
    private Long availableCopies;
}
