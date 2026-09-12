package com.school.library.controller;

import com.school.library.exception.BusinessException;
import com.school.library.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传（图书封面等）。
 *
 * <p>上传的文件落盘到 {@code app.upload.dir} 配置目录（按类型分子目录），
 * 返回以 {@code /uploads/**} 开头的访问 URL，由 WebConfig 的静态资源映射对外提供。
 */
@RestController
@RequestMapping("/api/uploads")
@Tag(name = "文件上传", description = "本地图片上传（封面等）")
public class FileUploadController {

    /** 允许的图片类型 → 扩展名（按 Content-Type 白名单校验，文件名由服务端重新生成） */
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    /** 封面大小上限（与 multipart 全局 10MB 独立，给出更明确的错误提示） */
    private static final long MAX_COVER_SIZE = 5 * 1024 * 1024L;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Operation(summary = "上传图书封面（管理员），返回可直接使用的图片 URL")
    @PostMapping("/cover")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadCover(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "请选择要上传的图片");
        }
        String ext = IMAGE_TYPES.get(file.getContentType());
        if (ext == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "仅支持 JPG / PNG / GIF / WebP 图片");
        }
        if (file.getSize() > MAX_COVER_SIZE) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "图片大小不能超过 5MB");
        }

        try {
            Path dir = Paths.get(uploadDir, "covers").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            // 文件名服务端生成（UUID），不使用原始文件名，避免路径拼接与重名问题
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return ResponseEntity.ok(Map.of("url", "/uploads/covers/" + filename));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED, "图片上传失败，请稍后重试");
        }
    }
}
