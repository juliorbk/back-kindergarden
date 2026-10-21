package com.ueclca.backend.dtos;

import java.util.List;
import java.util.Set;

import com.ueclca.backend.models.Channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String title,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 4096, message = "La descripción no puede superar los 4096 caracteres")
        String caption,

        @Size(max = 500, message = "La URL de la imagen no puede superar los 500 caracteres")
        @Pattern(regexp = "^https?://\\S+$", message = "La imagen debe ser una URL http(s) válida")
        String imageUrl,

        @Size(max = 10, message = "La publicación no puede tener más de 10 medios")
        List<Long> mediaIds,

        @NotEmpty(message = "Selecciona al menos un canal")
        Set<Channel> channels) {
}
