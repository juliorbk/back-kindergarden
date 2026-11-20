package com.ueclca.backend.dtos;

import java.time.Instant;
import java.time.LocalDate;

import com.ueclca.backend.models.Enrollment;
import com.ueclca.backend.models.Guardian;
import com.ueclca.backend.models.GuardianRelationship;

public record EnrollmentSummaryResponse(
        Long id,
        String schoolYear,
        String shift,
        String groupName,
        LocalDate enrollmentDate,
        Long studentId,
        String studentFullName,
        LocalDate studentBirthDate,
        String studentSex,
        String motherName,
        String fatherName,
        boolean hasPhoto,
        Instant createdAt,
        Instant updatedAt) {

    public static EnrollmentSummaryResponse from(Enrollment enrollment) {
        String studentFullName = enrollment.getStudent().getLastNames() + ", "
                + enrollment.getStudent().getFirstNames();

        return new EnrollmentSummaryResponse(
                enrollment.getId(),
                enrollment.getSchoolYear(),
                enrollment.getShift().name(),
                enrollment.getGroupName().name(),
                enrollment.getEnrollmentDate(),
                enrollment.getStudent().getId(),
                studentFullName,
                enrollment.getStudent().getBirthDate(),
                enrollment.getStudent().getSex().name(),
                guardianName(enrollment, GuardianRelationship.MOTHER),
                guardianName(enrollment, GuardianRelationship.FATHER),
                enrollment.getPhotoDataUrl() != null,
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt());
    }

    private static String guardianName(Enrollment enrollment, GuardianRelationship relationship) {
        return enrollment.getStudent().getGuardians().stream()
                .filter(guardian -> guardian.getRelationship() == relationship)
                .map(Guardian::getFullName)
                .findFirst()
                .orElse(null);
    }
}
