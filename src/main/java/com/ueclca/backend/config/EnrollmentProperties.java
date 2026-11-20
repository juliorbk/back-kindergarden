package com.ueclca.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.enrollment")
public record EnrollmentProperties(
        @Positive(message = "ENROLLMENT_MAX_DOCUMENT_SIZE_BYTES debe ser mayor que cero")
        long maxDocumentSizeBytes) {
}
