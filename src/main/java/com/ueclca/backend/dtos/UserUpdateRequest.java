package com.ueclca.backend.dtos;

import com.ueclca.backend.models.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no debe superar los 150 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        Role role,

        @NotNull(message = "El estado habilitado es obligatorio")
        Boolean enabled) {
}
