package com.school.library.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 会话空闲超时策略。
 *
 * 超时时长只在这里读一次，供三处共用，避免多处各写一个数字而对不上：
 * 1. {@link com.school.library.security.LoginSessionManager}：建会话时设置 maxInactiveInterval（Redis 过期时间）；
 * 2. {@link com.school.library.security.CompositeHttpSessionIdResolver}：设置会话 Cookie 的 Max-Age；
 * 3. {@link com.school.library.security.SessionTimeoutInterceptor}：判定「空闲是否已超时」。
 *
 * 配置项：{@code app.session.timeout-minutes}（默认 30 分钟）。
 * 注意：它必须与 {@code spring.session.timeout} 保持一致，后者是 Spring Session 对
 * 「非登录流程创建的会话」的默认过期时间（本项目的会话都在登录时创建并显式设置了间隔）。
 */
@Component
public class SessionTimeoutPolicy {

    private final long timeoutMinutes;

    public SessionTimeoutPolicy(@Value("${app.session.timeout-minutes:30}") long timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    /** 空闲超时分钟数 */
    public long timeoutMinutes() {
        return timeoutMinutes;
    }

    /** 空闲超时时长 */
    public Duration timeout() {
        return Duration.ofMinutes(timeoutMinutes);
    }

    /** 空闲超时的毫秒数（拦截器做时间差比较用） */
    public long timeoutMillis() {
        return timeout().toMillis();
    }
}
