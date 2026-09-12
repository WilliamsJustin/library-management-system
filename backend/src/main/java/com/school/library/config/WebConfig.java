package com.school.library.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Web 静态资源配置：把 {@code /uploads/**} 映射到本地上传目录
 * （{@code app.upload.dir}，默认工作目录下的 uploads），供上传的封面图片对外访问。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * 启动时先创建上传目录。不能省：Path.toUri() 只对「已存在的目录」才补尾部斜杠，
     * 而 addResourceLocations 的路径必须以 / 结尾，否则 /uploads/** 全部 404。
     */
    @PostConstruct
    void prepareUploadDir() throws Exception {
        Files.createDirectories(Paths.get(uploadDir, "covers").toAbsolutePath().normalize());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 统一转绝对路径，避免工作目录变化导致图片 404；并确保以 / 结尾
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
