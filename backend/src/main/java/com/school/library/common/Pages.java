package com.school.library.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数换算。
 *
 * <p>前端统一使用 0 基页码（{@code ?page=0&size=10}，第一页是 0），
 * 而 MyBatis-Plus 的 {@code Page#current} 是 1 基，转换集中在这里，避免每个 Service 各写一遍而漏掉 +1。
 */
public final class Pages {

    /** 未传 size 时的默认每页条数 */
    public static final int DEFAULT_SIZE = 10;

    /** 每页条数上限，防止一次拉取过多数据 */
    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    /** 把前端的 0 基 page/size 转成 MyBatis-Plus 的分页对象（1 基页码） */
    public static <T> Page<T> of(int page, int size) {
        long current = Math.max(page, 0) + 1L;
        long pageSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new Page<>(current, pageSize);
    }
}
