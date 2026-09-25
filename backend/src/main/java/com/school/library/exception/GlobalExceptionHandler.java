package com.school.library.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 统一错误响应结构：{ "code": "...", "message": "..." }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.badRequest()
                .body(Map.of("code", ErrorCodes.VALIDATION_FAILED, "message", message));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", ErrorCodes.CONFLICT, "message", "数据冲突：唯一字段重复或存在关联记录"));
    }

    /**
     * 未知路由：Spring Boot 3.2（Spring 6.1+）下，不存在的路径会先落入静态资源处理器并抛出
     * NoResourceFoundException；NoHandlerFoundException 则在显式开启 throw-exception-if-no-handler-found
     * 时出现。两者都应映射为 404，而不是落入兜底的 500。
     */
    @ExceptionHandler({
            org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleRouteNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", ErrorCodes.NOT_FOUND, "message", "请求的资源不存在"));
    }

    /**
     * 权限不足：@PreAuthorize 在 controller 方法内抛出的 AccessDeniedException 会被
     * 本 @RestControllerAdvice 先于 Security 的 accessDeniedHandler 捕获，
     * 若不单独处理会落入兜底 500。这里显式映射为 403。
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", ErrorCodes.FORBIDDEN, "message", "无权限访问该资源"));
    }

    /**
     * 会话存储（Redis）连不上。
     *
     * 触发路径：登录成功后会话要写进 Redis，SessionRepositoryFilter 在响应提交时调用
     * RedisSessionRepository.save()，连接失败会抛 RedisConnectionFailureException 并沿过滤器链
     * 冒泡回 DispatcherServlet，最终落到本 advice。
     *
     * 这是最常见的「部署事故」——忘了启动 Redis。若走兜底 500，使用者只看到「服务器内部错误」，
     * 完全无法定位；因此单独映射为 503 并给出可操作提示。
     */
    @ExceptionHandler(org.springframework.data.redis.RedisConnectionFailureException.class)
    public ResponseEntity<Map<String, String>> handleSessionStoreUnavailable(Exception ex) {
        log.error("会话存储 Redis 不可用：请确认 Redis 已启动（默认 127.0.0.1:6379）", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "SESSION_STORE_UNAVAILABLE",
                        "message", "会话服务不可用：无法连接 Redis（默认 127.0.0.1:6379），请先启动 Redis 再登录"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        // 兜底 500 必须留下堆栈：否则线上只有一句「服务器内部错误」，无从定位
        log.error("未预期的异常，已按 500 返回", ex);
        return ResponseEntity.internalServerError()
                .body(Map.of("code", "INTERNAL_ERROR", "message", "服务器内部错误"));
    }
}
