package com.ueclca.backend.dtos;

import com.ueclca.backend.models.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no debe superar los 150 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        Role role) {
}
