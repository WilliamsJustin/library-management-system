package com.school.library.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>{@code @MapperScan} 扫描 mapper 包，省去在每个接口上写 {@code @Mapper}。
 *
 * <p>插件注意点：
 * <ul>
 *   <li>{@link PaginationInnerInterceptor} 是分页的基础，不注册它的话
 *       {@code selectPage} 不会拼 LIMIT、{@code total} 永远是 0。</li>
 *   <li>{@link OptimisticLockerInnerInterceptor} 接管实体上 {@code @Version} 字段，
 *       让 {@code updateById(entity)} 自动带上 {@code where version = ?} 并把 version 加一，
 *       用于替代原 JPA 的 {@code @Version}（借出/归还的并发保护）。</li>
 *   <li>两者注册的相对顺序不影响功能（各自匹配不同的 SQL 类型）。</li>
 * </ul>
 */
@Configuration
@MapperScan("com.school.library.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁：作用于带 @Version 的实体更新（book_copy / loan）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 分页：作用于 IPage 入参的查询
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
