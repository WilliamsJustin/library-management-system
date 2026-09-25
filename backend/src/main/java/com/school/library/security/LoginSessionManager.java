package com.school.library.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * 登录会话的建立与销毁（认证由「JWT 无状态」改为「Spring Session 服务端会话」的落点）。
 *
 * 流程：登录成功 → 新建 HttpSession（由 Spring Session 落到 Redis）→ 把 SecurityContext 写入会话。
 * 之后每个请求由 Spring Security 的 SecurityContextHolderFilter 自动从会话恢复认证信息，
 * 不再需要自定义过滤器解析令牌。
 *
 * 这样「会话超时」的判定权完全在服务端：Redis 里的会话一旦过期（或被
 * {@link SessionTimeoutInterceptor} 提前作废），任何请求都会拿不到认证信息而返回 401。
 */
@Component
public class LoginSessionManager {

    private final SecurityContextRepository securityContextRepository;
    private final SessionTimeoutPolicy timeoutPolicy;

    public LoginSessionManager(SecurityContextRepository securityContextRepository,
                              SessionTimeoutPolicy timeoutPolicy) {
        this.securityContextRepository = securityContextRepository;
        this.timeoutPolicy = timeoutPolicy;
    }

    /**
     * 建立会话并写入认证信息，返回会话 ID（前端把它当登录凭证，见 dto/LoginResponse 的 token 字段）。
     */
    public String startSession(AppPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        // 防会话固定攻击：先作废可能存在的旧会话（上一位登录者 / 匿名会话），再建全新的会话。
        // 这里不能省——本项目的登录是手写流程，不走 Spring Security 的登录过滤器，
        // 因此 SessionAuthenticationStrategy 的 changeSessionId 不会自动生效。
        HttpSession previous = request.getSession(false);
        if (previous != null) {
            previous.invalidate();
        }

        // 注意：setDetails 只在具体实现类上（Authentication 接口只有 getDetails），所以这里声明为具体类型
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // saveContext 已创建会话，这里取回同一个会话设置活跃时间与过期时间
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionTimeoutInterceptor.LAST_ACTIVITY_ATTR, System.currentTimeMillis());
        // 空闲超时：会话在 Redis 里的过期时间就是它，每次请求都会被拦截器刷新
        session.setMaxInactiveInterval((int) timeoutPolicy.timeout().toSeconds());
        return session.getId();
    }

    /**
     * 退出登录：作废服务端会话并清空当前认证。
     * 会话 Cookie 由 Spring Session 的 SessionRepositoryFilter 在会话失效后自动清除。
     */
    public void endSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * 会话中存放 SecurityContext 的键名，供测试与排查使用。
     */
    public static String securityContextKey() {
        return HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
    }
}
