package com.school.library.dto;

public record LoginResponse(String token, Long userId, String name, String role, String readerType) {
}
