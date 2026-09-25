package com.school.library.config;

import com.school.library.security.SessionTimeoutInterceptor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Web 配置：静态资源映射 + 会话超时拦截器注册。
 *
 * <p>静态资源：把 {@code /uploads/**} 映射到本地上传目录
 * （{@code app.upload.dir}，默认工作目录下的 uploads），供上传的封面图片对外访问。
 *
 * <p>拦截器：{@link SessionTimeoutInterceptor} 只挂 /api/**，且排除登录、注册、退出登录三个
 * 「会话生命周期入口」——会话已超时时，恰恰要靠重新登录（或退出）来恢复，不能被拦截器先挡掉。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final SessionTimeoutInterceptor sessionTimeoutInterceptor;

    public WebConfig(SessionTimeoutInterceptor sessionTimeoutInterceptor) {
        this.sessionTimeoutInterceptor = sessionTimeoutInterceptor;
    }

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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionTimeoutInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/logout");
    }
}
