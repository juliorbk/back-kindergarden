package com.ueclca.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ueclca.backend.models.StudentSex;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentRequest(
        @NotBlank(message = "Los apellidos del alumno son obligatorios")
        @Size(max = 150, message = "Los apellidos no pueden superar los 150 caracteres")
        String lastNames,

        @NotBlank(message = "Los nombres del alumno son obligatorios")
        @Size(max = 150, message = "Los nombres no pueden superar los 150 caracteres")
        String firstNames,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate birthDate,

        @Size(max = 150, message = "El lugar de nacimiento no puede superar los 150 caracteres")
        String birthPlace,

        @NotBlank(message = "La nacionalidad es obligatoria")
        @Size(max = 80, message = "La nacionalidad no puede superar los 80 caracteres")
        String nationality,

        @NotNull(message = "El sexo es obligatorio")
        StudentSex sex,

        @DecimalMin(value = "30.0", message = "La talla debe estar entre 30 y 200 cm")
        @DecimalMax(value = "200.0", message = "La talla debe estar entre 30 y 200 cm")
        @Digits(integer = 3, fraction = 1, message = "La talla admite como máximo un decimal")
        BigDecimal heightCm,

        @DecimalMin(value = "5.0", message = "El peso debe estar entre 5 y 120 kg")
        @DecimalMax(value = "120.0", message = "El peso debe estar entre 5 y 120 kg")
        @Digits(integer = 3, fraction = 1, message = "El peso admite como máximo un decimal")
        BigDecimal weightKg,

        @Size(max = 50, message = "El número de registro escolar no puede superar los 50 caracteres")
        String schoolRegistryNumber,

        @NotBlank(message = "El teléfono del alumno es obligatorio")
        @Pattern(
                regexp = "^0(4(12|14|16|24|26)|2\\d{2})-?\\d{7}$",
                message = "Teléfono venezolano inválido (ej. 0414-1234567)")
        String phone,

        @NotBlank(message = "La dirección del alumno es obligatoria")
        @Size(max = 500, message = "La dirección no puede superar los 500 caracteres")
        String address) {
}
