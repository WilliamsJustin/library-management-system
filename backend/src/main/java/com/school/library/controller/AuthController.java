package com.school.library.controller;

import com.school.library.dto.ChangePasswordRequest;
import com.school.library.dto.LoginRequest;
import com.school.library.dto.LoginResponse;
import com.school.library.dto.RegisterRequest;
import com.school.library.security.AppPrincipal;
import com.school.library.security.LoginSessionManager;
import com.school.library.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * 会话由 Spring Session 管理（数据存 Redis）：登录/注册成功后建立会话并返回会话 ID，
 * 退出登录则作废服务端会话。会话的空闲超时由 SessionTimeoutInterceptor 判定。
 */
@Tag(name = "认证", description = "登录 / 注册 / 退出登录 / 修改密码")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginSessionManager loginSessionManager;

    public AuthController(AuthService authService, LoginSessionManager loginSessionManager) {
        this.authService = authService;
        this.loginSessionManager = loginSessionManager;
    }

    @Operation(summary = "登录", description = "校验账号密码并建立服务端会话，返回会话 ID（前端作为登录凭证保存）")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        AppPrincipal principal = authService.authenticate(request);
        String sessionId = loginSessionManager.startSession(principal, httpRequest, httpResponse);
        return LoginResponse.of(sessionId, principal);
    }

    @Operation(summary = "读者自助注册", description = "注册成功即建立会话，实现自动登录")
    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        AppPrincipal principal = authService.register(request);
        String sessionId = loginSessionManager.startSession(principal, httpRequest, httpResponse);
        return LoginResponse.of(sessionId, principal);
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    @Operation(summary = "退出登录",
            description = "作废服务端会话（Redis 中的会话随之删除）；会话已失效时同样返回成功，便于前端清理本地登录态")
    @PostMapping("/logout")
    public void logout(HttpServletRequest httpRequest) {
        loginSessionManager.endSession(httpRequest);
    }
}
