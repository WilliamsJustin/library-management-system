package com.school.library.config;

import com.school.library.security.CompositeHttpSessionIdResolver;
import com.school.library.security.SessionTimeoutPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

/**
 * 会话相关的基础设施装配（Spring Session + Redis）。
 *
 * 会话数据落在 Redis（由 spring-session-data-redis 自动装配 RedisSessionRepository），
 * 这里只负责两件事：会话 ID 走哪条通道、认证信息存在哪。
 */
@Configuration
public class SessionConfig {

    /**
     * 会话 ID 解析器：Cookie + {@code Authorization: Bearer <会话ID>} 双通道。
     *
     * 显式声明这个 bean 后，Spring Session 的 SpringHttpSessionConfiguration 会因
     * {@code @ConditionalOnMissingBean(HttpSessionIdResolver.class)} 让位，默认的
     * CookieHttpSessionIdResolver 不再生效（见 security/CompositeHttpSessionIdResolver 的说明）。
     */
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver(SessionTimeoutPolicy timeoutPolicy) {
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName("LIBRARY_SESSION");
        cookieSerializer.setCookiePath("/");
        cookieSerializer.setUseHttpOnlyCookie(true);
        cookieSerializer.setSameSite("Lax");
        // 与后端空闲超时保持一致：关掉浏览器超过这个时间再打开，浏览器也不会再带着失效的会话 Cookie
        cookieSerializer.setCookieMaxAge((int) timeoutPolicy.timeout().toSeconds());
        return new CompositeHttpSessionIdResolver(cookieSerializer);
    }

    /**
     * 认证信息的存储位置：HttpSession（即 Spring Session 的 Redis 会话）。
     *
     * Spring Security 6 起默认不再自动把 SecurityContext 写回会话，必须显式声明：
     * 写入用 {@code LoginSessionManager#startSession}，读取由 SecurityContextHolderFilter 自动完成。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
