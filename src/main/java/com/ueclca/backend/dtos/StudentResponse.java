package com.ueclca.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.ueclca.backend.models.Student;

public record StudentResponse(
        Long id,
        String firstNames,
        String lastNames,
        LocalDate birthDate,
        String birthPlace,
        String nationality,
        String sex,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String schoolRegistryNumber,
        String phone,
        String address) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getFirstNames(),
                student.getLastNames(),
                student.getBirthDate(),
                student.getBirthPlace(),
                student.getNationality(),
                student.getSex().name(),
                student.getHeightCm(),
                student.getWeightKg(),
                student.getSchoolRegistryNumber(),
                student.getPhone(),
                student.getAddress());
    }
}
