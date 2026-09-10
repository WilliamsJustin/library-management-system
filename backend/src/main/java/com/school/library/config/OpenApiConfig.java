package com.school.library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档（Knife4j / springdoc-openapi）基础信息配置。
 *
 * <p>仅用于给文档页面提供标题、版本，以及一个全局的 Bearer Token 输入框，
 * 方便直接在文档页面调通需要登录的接口（先调 /api/auth/login 拿 token，再点「Authorize」填入）。
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("学校图书馆借阅系统 API")
                        .description("图书编目、读者管理、借阅流通、逾期罚款、公告管理、活动管理等接口文档")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
