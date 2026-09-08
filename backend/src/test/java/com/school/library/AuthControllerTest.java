package com.school.library;

import com.school.library.dto.LoginRequest;
import com.school.library.dto.LoginResponse;
import com.school.library.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestData.class)
class AuthControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestData testData;

    @Test
    void loginSuccess() {
        testData.admin("admin1");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest("admin1", TestData.DEFAULT_PASSWORD),
                LoginResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    void loginFailure() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest("admin1", "wrong-password"),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("code")).isEqualTo("BAD_CREDENTIALS");
    }
}