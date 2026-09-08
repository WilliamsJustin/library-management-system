package com.school.library.controller;

import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查 + 统一错误结构的验证端点。
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        return Map.of("message", "pong");
    }

    /** 仅供验证统一错误响应结构：GET /api/ping/error?fail=true */
    @GetMapping("/api/ping/error")
    public Map<String, String> pingError(@RequestParam(defaultValue = "true") boolean fail) {
        if (fail) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "演示错误：资源不存在");
        }
        return Map.of("message", "ok");
    }
}
