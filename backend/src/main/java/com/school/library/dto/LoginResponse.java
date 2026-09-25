package com.school.library.dto;

import com.school.library.security.AppPrincipal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应。
 *
 * token 字段名保持不变（前端 types/index.ts 与 stores/auth.ts 依赖它），
 * 但内容已由「JWT」改为「Spring Session 的会话 ID」：会话数据存在 Redis，
 * 空闲超过 app.session.timeout-minutes 分钟即失效（见 security/SessionTimeoutInterceptor）。
 */
public record LoginResponse(
        @Schema(description = "登录凭证：Spring Session 会话 ID（会话数据存 Redis，空闲超时后失效）",
                example = "b1f3c0d2-6f1a-4b1e-9a5c-2f7d8e0c4a11")
        String token,
        @Schema(description = "用户 ID")
        Long userId,
        @Schema(description = "姓名")
        String name,
        @Schema(description = "角色：ADMIN / READER")
        String role,
        @Schema(description = "读者类型：STUDENT / TEACHER")
        String readerType) {

    /** 用「会话 ID + 认证主体」组装登录响应，避免各处重复拼装 */
    public static LoginResponse of(String sessionId, AppPrincipal principal) {
        return new LoginResponse(sessionId, principal.userId(), principal.name(),
                principal.role().name(), principal.readerType());
    }
}
