package com.ueclca.backend.dtos;

import com.ueclca.backend.models.EnrollmentDocumentType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentDocumentRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        EnrollmentDocumentType documentType,

        boolean presented,

        @Size(max = 255, message = "El nombre del archivo no puede superar los 255 caracteres")
        String fileName,

        @Size(max = 255, message = "La referencia del archivo no puede superar los 255 caracteres")
        String storageKey,

        @Size(max = 100, message = "El tipo de archivo no puede superar los 100 caracteres")
        String mimeType,

        Long sizeBytes) {
}
