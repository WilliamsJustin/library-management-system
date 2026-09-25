package com.school.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置。
 *
 * 认证方式：服务端会话（HttpSession + Spring Session Redis），不再使用 JWT。
 * - 登录：AuthController 校验账号密码后由 LoginSessionManager 建立会话（写 SecurityContext）；
 * - 后续请求：SecurityContextHolderFilter 依据 securityContextRepository 从会话恢复认证信息；
 * - 会话超时：自定义拦截器 SessionTimeoutInterceptor 判定空闲超时并作废会话，
 *   同时 spring.session.timeout 决定 Redis 里会话的兜底过期时间。
 * 因此这里必须是 IF_REQUIRED（需要时才建会话），而不能是原来的 STATELESS。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 接口文档相关路径，统一对匿名开放（否则 doc.html / api-docs 会被拦成 401）。
     * webjars 承载 Knife4j、Swagger UI 的前端静态资源；swagger-resources 为 Knife4j 兼容旧版路径。
     */
    private static final String[] DOC_ENDPOINTS = {
            "/doc.html",
            "/webjars/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-resources/**",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 会话型认证：认证信息存 HttpSession（Spring Session → Redis）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // 前后端分离的 REST API 不需要「记住原请求、登录后跳回」，关掉可避免匿名请求被写入会话
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/ping/**").permitAll()
                        // 退出登录必须匿名可访问：会话已失效时也要能返回 200，让前端安心清理本地登录态
                        .requestMatchers("/api/auth/logout").permitAll()
                        // 错误页不要求登录：否则任何异常都可能被二次拦成 401，把真正的错误信息盖掉
                        .requestMatchers("/error").permitAll()
                        // 接口文档（Knife4j 4.x + springdoc-openapi 2.x）必须放行，否则 doc.html 的第一跳
                        // /v3/api-docs/swagger-config 会被 anyRequest().authenticated() 拦成 401，页面直接白屏。
                        // /webjars/** 承载 Knife4j 与 Swagger UI 的前端静态资源，同样不能要求登录。
                        .requestMatchers(DOC_ENDPOINTS).permitAll()
                        // 公共站点：书目检索、公告、读者自助注册均对匿名开放
                        .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/announcements/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/activities/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        // 帮助与反馈：FAQ 检索与提交留言对匿名开放（前台悬浮窗未登录也能用）；
                        // 其余（我的留言/对话/管理接口）仍要求登录或管理员角色
                        .requestMatchers(HttpMethod.GET, "/api/help/faq").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/help/feedback").permitAll()
                        // 上传的封面图片是公共站点资源（前台书目展示不要求登录）
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            // 必须显式指定 UTF-8，否则 Windows 下默认 ISO-8859-1 会把中文写成 "???"
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"未登录\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"无权限\"}");
                        }));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 允许 Vite 开发服务器跨域访问 API */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
