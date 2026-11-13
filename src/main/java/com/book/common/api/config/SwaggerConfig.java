package com.book.common.api.config;

import com.book.common.api.RequiresSecurity;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

@Configuration
public class SwaggerConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .components(new Components().addSecuritySchemes(BEARER_AUTH,
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .info(new Info().title("Demo API").version("1.0.0"));
    }

    @Bean
    GlobalOperationCustomizer securityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(RequiresSecurity.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequiresSecurity.class)) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                if (operation.getResponses() == null) {
                    operation.responses(new ApiResponses());
                }
                operation.getResponses().putIfAbsent("401", new ApiResponse().description("인증 정보가 유효하지 않습니다."));
                operation.getResponses().putIfAbsent("403", new ApiResponse().description("접근 권한이 없습니다."));
            }
            return operation;
        };
    }

    @Bean
    public GroupedOpenApi generalApi() {
        return GroupedOpenApi.builder().group("general").pathsToMatch("/api/**", "/admin-api/**").build();
    }
}
