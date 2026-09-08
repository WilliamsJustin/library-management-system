package com.school.library.security;

import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 从 SecurityContext 取当前认证主体的工具 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AppPrincipal get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppPrincipal principal) {
            return principal;
        }
        throw new BusinessException(ErrorCodes.UNAUTHORIZED, "未登录");
    }

    public static Long userId() {
        return get().userId();
    }
}
