package com.ueclca.backend.dtos;

import com.ueclca.backend.models.EnrollmentDocument;

public record EnrollmentDocumentResponse(
        Long id,
        String documentType,
        boolean presented,
        String fileName,
        String storageKey,
        String mimeType,
        Long sizeBytes) {

    public static EnrollmentDocumentResponse from(EnrollmentDocument document) {
        return new EnrollmentDocumentResponse(
                document.getId(),
                document.getDocumentType().name(),
                document.isPresented(),
                document.getFileName(),
                document.getStorageKey(),
                document.getMimeType(),
                document.getSizeBytes());
    }
}
