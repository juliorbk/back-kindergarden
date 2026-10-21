package com.ueclca.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @NotEmpty(message = "CORS_ALLOWED_ORIGINS debe incluir al menos un origen")
        List<@NotBlank(message = "Cada origen CORS debe ser una URL válida") String> allowedOrigins) {
}
