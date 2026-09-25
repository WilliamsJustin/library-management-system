package com.school.library;

import com.school.library.dto.LoginRequest;
import com.school.library.dto.LoginResponse;
import com.school.library.entity.ReaderType;
import com.school.library.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务端会话（Spring Session）的集成测试：认证由 JWT 改为会话后，
 * 「怎么拿到会话」「会话怎么失效」这两件事必须真的成立。
 *
 * 会话存储由 support/InMemorySessionConfig 提供（测试不依赖 Redis），
 * 但过滤器链、会话解析（Cookie 与 Authorization: Bearer 两条通道）、会话失效都是真实链路。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestData.class)
class AuthSessionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestData testData;

    @Test
    void loginCreatesSessionAndLogoutInvalidatesIt() {
        testData.reader("session-reader", ReaderType.STUDENT);

        // ---- 1. 登录：建立服务端会话，返回会话 ID 并下发会话 Cookie ----
        ResponseEntity<LoginResponse> login = restTemplate.postForEntity("/api/auth/login",
                new LoginRequest("session-reader", TestData.DEFAULT_PASSWORD), LoginResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        String sessionId = login.getBody().token();
        assertThat(sessionId).isNotBlank();

        String sessionCookie = sessionCookieOf(login);
        assertThat(sessionCookie).startsWith("LIBRARY_SESSION=");

        // ---- 2. Cookie 通道可用：浏览器同源访问走的就是这条路 ----
        assertThat(get("/api/loans/my", sessionCookie).getStatusCode()).isEqualTo(HttpStatus.OK);

        // ---- 3. 请求头通道可用：前端把会话 ID 放在 Authorization: Bearer 里发 ----
        HttpHeaders bearer = new HttpHeaders();
        bearer.setBearerAuth(sessionId);
        ResponseEntity<String> viaBearer = exchange("/api/loans/my", bearer);
        assertThat(viaBearer.getStatusCode()).as("响应体=%s", viaBearer.getBody()).isEqualTo(HttpStatus.OK);

        // ---- 4. 不带任何凭证访问受保护接口 → 401 ----
        assertThat(restTemplate.getForEntity("/api/loans/my", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        // 伪造的会话 ID 同样无效（会话存在服务端，客户端无法自证）
        HttpHeaders forged = new HttpHeaders();
        forged.setBearerAuth("forged-session-id");
        assertThat(exchange("/api/loans/my", forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // ---- 5. 退出登录：服务端会话立即作废，原来那套凭证随之失效 ----
        assertThat(post("/api/auth/logout", cookieHeaders(sessionCookie)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get("/api/loans/my", sessionCookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        HttpHeaders afterLogout = new HttpHeaders();
        afterLogout.setBearerAuth(sessionId);
        assertThat(exchange("/api/loans/my", afterLogout).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** 从登录响应的 Set-Cookie 里取出「名字=值」部分，供后续请求带上 */
    private String sessionCookieOf(ResponseEntity<LoginResponse> login) {
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        int end = setCookie.indexOf(';');
        return end > 0 ? setCookie.substring(0, end) : setCookie;
    }

    private HttpHeaders cookieHeaders(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return headers;
    }

    private ResponseEntity<String> get(String url, String cookie) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(cookieHeaders(cookie)), String.class);
    }

    private ResponseEntity<String> exchange(String url, HttpHeaders headers) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    /** 退出登录是 POST 接口，不能用上面的 GET 版 exchange，否则会 405/500 */
    private ResponseEntity<String> post(String url, HttpHeaders headers) {
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }
}
