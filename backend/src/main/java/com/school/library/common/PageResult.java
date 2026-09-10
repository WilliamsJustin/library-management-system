package com.school.library.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

/**
 * 统一的分页返回结构。
 *
 * <p>持久层换成 MyBatis-Plus 后，分页对象也换成了 {@link IPage}，但它的 JSON 结构是
 * {@code {records, total, size, current, pages}}，与前端约定了的
 * {@code {content, totalElements, ...}} 不一致。为了不改动前端（前端读
 * {@code data.content} 与 {@code data.totalElements}），这里保留 Spring Data {@code Page}
 * 的字段命名，只是内部由 {@link IPage} 转换而来。
 *
 * <p>{@code number} 采用 0 基（第一页为 0），与前端传参 {@code ?page=0&size=10} 保持一致；
 * MyBatis-Plus 的 {@code current} 是 1 基，转换时已减一。
 */
@Getter
public class PageResult<T> {

    /** 当前页数据 */
    private final List<T> content;

    /** 总记录数 */
    private final long totalElements;

    /** 总页数 */
    private final int totalPages;

    /** 当前页码，0 基 */
    private final int number;

    /** 每页条数 */
    private final int size;

    /** 当前页实际记录数 */
    private final int numberOfElements;

    private final boolean first;

    private final boolean last;

    private final boolean empty;

    private PageResult(List<T> content, long totalElements, int size, int number) {
        this.content = content;
        this.totalElements = totalElements;
        this.size = size;
        this.number = number;
        this.numberOfElements = content.size();
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        this.first = number == 0;
        this.last = number >= this.totalPages - 1;
        this.empty = content.isEmpty();
    }

    /** 直接把实体分页映射成响应体分页 */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> mapper) {
        List<T> content = page.getRecords().stream().map(mapper).toList();
        return new PageResult<>(content, page.getTotal(), (int) page.getSize(), (int) page.getCurrent() - 1);
    }

    /** 无需转换字段时使用 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page, Function.identity());
    }

    /** 由已转换好的列表 + 总数构造（用于需要二次组装响应体的场景） */
    public static <T> PageResult<T> of(IPage<?> page, List<T> content) {
        return new PageResult<>(content, page.getTotal(), (int) page.getSize(), (int) page.getCurrent() - 1);
    }
}
