package com.definex.survey.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI ayarları. Doküman koddaki controller'lardan üretilir; sözleşmenin asıl kaynağı
 * {@code docs/openapi/} altındaki YAML'dır. Yalnızca dev profilinde açıktır.
 *
 * <p>Korumalı uçları denemek için Swagger UI'da "Authorize" ile {@code bearerAuth} alanına access token girilir.
 * Herkese açık uçlarda (login/refresh) {@code @SecurityRequirements} ile gereksinim kaldırılır.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Survey Tool API", version = "v1"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
