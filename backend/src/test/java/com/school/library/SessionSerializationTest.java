package com.school.library;

import com.school.library.entity.UserRole;
import com.school.library.security.AppPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话存 Redis 时，Spring Session 的 RedisSessionRepository 默认使用 **JDK 序列化**。
 *
 * 而集成测试（AuthSessionTest）用的 MapSessionRepository 是把 Session 对象直接放进 Map、
 * 不做任何序列化——所以「登录接口返回 200」并不等于「会话真的能写进 Redis」。
 * 一旦 SecurityContext 里混进不可序列化的字段，真实环境会在登录那一步抛 NotSerializableException，
 * 而这种错误在内存实现下永远不会暴露。
 *
 * 这里做一次真实的 JDK 序列化往返，把这类问题提前拦在单元测试里（无需 Redis）。
 */
class SessionSerializationTest {

    @Test
    void securityContextWithAppPrincipalSurvivesJdkSerialization() throws Exception {
        AppPrincipal principal = new AppPrincipal(1L, "student1", "张同学", UserRole.READER, "STUDENT");

        // 与 LoginSessionManager#startSession 写入会话的完全同构
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetails(new MockHttpServletRequest()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContext restored = (SecurityContext) roundTrip(context);

        assertThat(restored.getAuthentication()).isNotNull();
        assertThat(restored.getAuthentication().getPrincipal()).isEqualTo(principal);
        assertThat(restored.getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_READER");
    }

    /** 会话里除 SecurityContext 之外，还会存拦截器的「最后活跃时间」——它同样必须可序列化 */
    @Test
    void sessionAttributesWrittenByInterceptorAreSerializable() throws Exception {
        assertThat(roundTrip(System.currentTimeMillis())).isInstanceOf(Long.class);
    }

    private Object roundTrip(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }
}
