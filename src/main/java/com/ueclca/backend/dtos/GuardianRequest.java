package com.ueclca.backend.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardianRequest(
        @Size(max = 150, message = "El nombre y apellido no puede superar los 150 caracteres")
        String fullName,

        @Pattern(
                regexp = "^[VE]-?\\d{6,9}$",
                message = "Cédula venezolana inválida (formato V-12345678 o E-12345678)")
        String nationalId,

        @Size(max = 500, message = "La dirección no puede superar los 500 caracteres")
        String address,

        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate birthDate,

        @Size(max = 150, message = "La profesión u oficio no puede superar los 150 caracteres")
        String occupation,

        Boolean works,

        @Size(max = 150, message = "El nombre de la empresa no puede superar los 150 caracteres")
        String employer,

        @Email(message = "El correo electrónico no es válido")
        @Size(max = 255, message = "El correo electrónico no puede superar los 255 caracteres")
        String email,

        @Pattern(
                regexp = "^0(4(12|14|16|24|26)|2\\d{2})-?\\d{7}$",
                message = "Teléfono venezolano inválido (ej. 0414-1234567)")
        String homePhone,

        @Pattern(
                regexp = "^0(4(12|14|16|24|26)|2\\d{2})-?\\d{7}$",
                message = "Teléfono venezolano inválido (ej. 0414-1234567)")
        String mobilePhone,

        @Pattern(
                regexp = "^0(4(12|14|16|24|26)|2\\d{2})-?\\d{7}$",
                message = "Teléfono venezolano inválido (ej. 0414-1234567)")
        String officePhone) {
}
