package com.ueclca.backend.dtos;

import java.time.Instant;

import com.ueclca.backend.models.EventCategory;
import com.ueclca.backend.models.EventKind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EventRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String title,

        @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
        String description,

        @Size(max = 255, message = "El lugar no puede superar los 255 caracteres")
        String location,

        @NotNull(message = "La categoría es obligatoria")
        EventCategory category,

        EventKind kind,

        @Size(max = 500, message = "La URL de la imagen no puede superar los 500 caracteres")
        @Pattern(regexp = "^https?://\\S+$", message = "La imagen debe ser una URL http(s) válida")
        String imageUrl,

        Boolean featured,

        Boolean allDay,

        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant startsAt,

        @NotNull(message = "La fecha de fin es obligatoria")
        Instant endsAt) {
}
