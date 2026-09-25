package com.school.library.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 自定义会话超时拦截器。
 *
 * 语义：距离「上次活跃」超过 {@code app.session.timeout-minutes} 分钟（默认 30）即视为会话超时——
 * 先作废会话、清空认证，再返回 401（{@code code=SESSION_EXPIRED}），前端据此跳回登录页。
 * 「关闭窗口后 30 分钟内不再打开就退出登录」就是这条规则：关掉页面后没有任何请求，
 * 最后活跃时间停在关闭那一刻，超过阈值后即使会话 Cookie 还在，也会被判为超时。
 *
 * 为什么除了 Spring Session 的 Redis 过期时间之外还要这个拦截器：
 * 1. 超时语义写在代码里：可测试、可日志、可随配置调整，不依赖 Redis 的过期精度与服务端配置；
 * 2. 每次请求刷新「最后活跃时间」并顺带刷新 Redis 里的 TTL，保证「只要还在操作就不会被登出」；
 * 3. 前端拿到的是明确的 SESSION_EXPIRED，而不是笼统的 401，能给出「会话已超时」的提示。
 */
@Component
public class SessionTimeoutInterceptor implements HandlerInterceptor {

    /** 会话中记录「最后活跃时间」（epoch 毫秒）的属性名；随会话一起存 Redis，多实例部署也能共享 */
    public static final String LAST_ACTIVITY_ATTR = "APP_LAST_ACTIVITY_AT";

    private static final Logger log = LogManager.getLogger(SessionTimeoutInterceptor.class);

    private final SessionTimeoutPolicy timeoutPolicy;

    public SessionTimeoutInterceptor(SessionTimeoutPolicy timeoutPolicy) {
        this.timeoutPolicy = timeoutPolicy;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            // 没有会话（匿名访问公开接口）：是否需要登录交给 Spring Security 判断
            return true;
        }

        long now = System.currentTimeMillis();
        Object recorded = session.getAttribute(LAST_ACTIVITY_ATTR);
        long lastActiveAt = (recorded instanceof Long value) ? value : session.getLastAccessedTime();

        if (now - lastActiveAt >= timeoutPolicy.timeoutMillis()) {
            log.info("会话空闲超时已注销：sessionId={} 空闲时长={} 分钟 请求={}",
                    session.getId(), (now - lastActiveAt) / 60000, request.getRequestURI());
            session.invalidate();
            SecurityContextHolder.clearContext();
            writeSessionExpired(response);
            return false;
        }

        // 本次请求算作一次活跃：刷新时间戳（同时把 Redis 里的会话过期时间往后推）
        session.setAttribute(LAST_ACTIVITY_ATTR, now);
        return true;
    }

    private void writeSessionExpired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        // 必须显式指定 UTF-8，否则 Windows 下默认 ISO-8859-1 会把中文写成 "???"
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write("{\"code\":\"SESSION_EXPIRED\",\"message\":\"会话已超时，请重新登录\"}");
    }
}
