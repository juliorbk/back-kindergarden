package com.ueclca.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET es obligatorio")
        @Size(min = 32, message = "JWT_SECRET debe tener al menos 32 caracteres")
        String secret,

        @NotBlank(message = "JWT_ISSUER es obligatorio")
        String issuer,

        @Positive(message = "JWT_EXPIRATION_MINUTES debe ser mayor que cero")
        long expirationMinutes) {
}
