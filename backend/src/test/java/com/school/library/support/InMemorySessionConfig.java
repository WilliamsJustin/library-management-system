package com.school.library.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试用的会话存储：用 Spring Session 自带的内存实现顶替 Redis 实现，测试因此不依赖外部 Redis。
 *
 * 为什么是 {@code @EnableSpringHttpSession} 而不只是声明一个 SessionRepository bean：
 * 生产环境 Boot 的 RedisSessionConfiguration 是 {@code @ConditionalOnMissingBean(SessionRepository.class)}，
 * 一旦我们提供了自己的 SessionRepository，它就会整体退让——连带 {@code @Import(RedisHttpSessionConfiguration)}
 * 也一起没了，而「注册 SessionRepositoryFilter（会话 Cookie 的读写、会话提交）」恰恰在那边。
 * 结果就是会话退回成容器原生 HttpSession（Cookie 变成 JSESSIONID），Spring Session 实际没生效。
 * 所以要自己把 Spring Session 的 Web 支持打开：{@code @EnableSpringHttpSession} 导入的
 * SpringHttpSessionConfiguration 会创建 SessionRepositoryFilter，并因
 * {@code @ConditionalOnMissingBean(HttpSessionIdResolver.class)} 让位给主代码里的
 * CompositeHttpSessionIdResolver —— 于是测试跑的是与生产一致的链路，只有存储换成了内存。
 *
 * 注意：本类在 {@code com.school.library} 包下，会被 @SpringBootTest 的组件扫描扫到，
 * 因此对所有测试生效，不需要逐个 @Import。
 */
@Configuration
@EnableSpringHttpSession
public class InMemorySessionConfig {

    /** 与 app.session.timeout-minutes 保持一致，避免测试会话的过期时间与业务规则脱节 */
    private static final Duration DEFAULT_MAX_INACTIVE_INTERVAL = Duration.ofMinutes(30);

    @Bean
    public MapSessionRepository sessionRepository() {
        MapSessionRepository repository = new MapSessionRepository(new ConcurrentHashMap<>());
        repository.setDefaultMaxInactiveInterval(DEFAULT_MAX_INACTIVE_INTERVAL);
        return repository;
    }
}
