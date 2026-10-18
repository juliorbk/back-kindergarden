package com.ueclca.backend.dtos;

import com.ueclca.backend.models.PublicationStatus;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "El estado es obligatorio")
        PublicationStatus status) {
}
