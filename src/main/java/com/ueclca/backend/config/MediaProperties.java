package com.ueclca.backend.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        @NotNull(message = "MEDIA_STORAGE_PATH es obligatorio")
        Path storagePath,

        @NotBlank(message = "MEDIA_PUBLIC_BASE_URL es obligatorio")
        String publicBaseUrl,

        @Positive(message = "MEDIA_MAX_FILE_SIZE_BYTES debe ser mayor que cero")
        long maxFileSizeBytes) {
}
