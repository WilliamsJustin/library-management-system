package com.school.library;

import com.school.library.security.SessionTimeoutInterceptor;
import com.school.library.security.SessionTimeoutPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话超时拦截器的单元测试：不启动 Spring 上下文，也不依赖 Redis。
 *
 * 覆盖「关掉窗口后 30 分钟不再打开就退出登录」这条规则的核心判定：
 * 最后活跃时间距今未超过阈值 → 放行并刷新活跃时间；超过阈值 → 作废会话并返回 401。
 */
class SessionTimeoutInterceptorTest {

    private static final long THIRTY_MINUTES = Duration.ofMinutes(30).toMillis();

    private final SessionTimeoutInterceptor interceptor =
            new SessionTimeoutInterceptor(new SessionTimeoutPolicy(30));

    @Test
    void passesThroughWhenRequestHasNoSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 匿名访问公开接口：没有会话就直接放行，是否需要登录交给 Spring Security
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void keepsSessionWhenIdleTimeIsWithinTimeout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        long lastActiveAt = System.currentTimeMillis() - THIRTY_MINUTES + Duration.ofMinutes(1).toMillis();
        session.setAttribute(SessionTimeoutInterceptor.LAST_ACTIVITY_ATTR, lastActiveAt);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/loans/my");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 空闲 29 分钟：还算活跃，放行并把活跃时间刷新到当前
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(session.isInvalid()).isFalse();
        assertThat((Long) session.getAttribute(SessionTimeoutInterceptor.LAST_ACTIVITY_ATTR))
                .isGreaterThan(lastActiveAt);
    }

    @Test
    void expiresSessionWhenIdleTimeExceedsTimeout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        long lastActiveAt = System.currentTimeMillis() - THIRTY_MINUTES - Duration.ofMinutes(1).toMillis();
        session.setAttribute(SessionTimeoutInterceptor.LAST_ACTIVITY_ATTR, lastActiveAt);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/loans/my");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 空闲 31 分钟：会话超时 —— 拦截器不放行、会话被作废、返回 401 SESSION_EXPIRED
        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("SESSION_EXPIRED");
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void fallsBackToLastAccessedTimeWhenActivityAttributeMissing() throws Exception {
        MockHttpSession session = new MockHttpSession();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 没有活跃时间属性（例如非登录流程创建的会话）时退回用会话自身的 lastAccessedTime 判定
        assertThat(session.getLastAccessedTime()).isGreaterThan(0);
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(session.getAttribute(SessionTimeoutInterceptor.LAST_ACTIVITY_ATTR)).isNotNull();
    }
}
