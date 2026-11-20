package com.ueclca.backend.dtos;

public record EnrollmentDocumentUploadResponse(
        String storageKey,
        String fileName,
        String mimeType,
        long sizeBytes) {
}
