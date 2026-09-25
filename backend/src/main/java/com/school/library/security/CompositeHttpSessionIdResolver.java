package com.school.library.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.util.List;

/**
 * 会话 ID 解析器：同时支持「会话 Cookie」与「Authorization: Bearer &lt;会话ID&gt;」两条通道。
 *
 * 为什么不用 Spring Session 默认的 CookieHttpSessionIdResolver：
 * 1. 本项目前端一直把登录凭证放在 localStorage 里、以 {@code Authorization: Bearer} 头发送。
 *    保留这条通道后，认证从 JWT 换成服务端会话时前端不需要改任何请求代码；
 *    同时用 curl / Postman 联调时直接带会话 ID 即可，不必维护 cookie jar。
 * 2. Cookie 通道照旧保留：浏览器同源访问（开发时的 Vite 代理、生产时的 Nginx 同源部署）会自动携带，
 *    这样「关掉浏览器再打开、只要没超过空闲超时」仍然保持登录态。
 *
 * 读取顺序是「先请求头、后 Cookie」：请求头是客户端显式指定的，优先于浏览器自动携带的 Cookie，
 * 避免同一浏览器里新旧会话互相干扰。
 */
public class CompositeHttpSessionIdResolver implements HttpSessionIdResolver {

    /** 请求头通道的前缀，与前端 api/http.ts 里发送的格式一致 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final CookieSerializer cookieSerializer;

    public CompositeHttpSessionIdResolver(CookieSerializer cookieSerializer) {
        this.cookieSerializer = cookieSerializer;
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        String fromHeader = resolveFromHeader(request);
        if (fromHeader != null) {
            return List.of(fromHeader);
        }
        return cookieSerializer.readCookieValues(request);
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        CookieSerializer.CookieValue cookieValue =
                new CookieSerializer.CookieValue(request, response, sessionId);
        cookieSerializer.writeCookieValue(cookieValue);
    }

    /** 会话作废时清除 Cookie（浏览器下次请求就不会再带失效的会话 ID） */
    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        CookieSerializer.CookieValue cookieValue =
                new CookieSerializer.CookieValue(request, response, "");
        cookieValue.setCookieMaxAge(0);
        cookieSerializer.writeCookieValue(cookieValue);
    }

    private String resolveFromHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String sessionId = header.substring(BEARER_PREFIX.length()).trim();
        return sessionId.isEmpty() ? null : sessionId;
    }
}
