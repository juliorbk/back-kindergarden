package com.ueclca.backend.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.ueclca.backend.models.Enrollment;
import com.ueclca.backend.models.Guardian;
import com.ueclca.backend.models.GuardianRelationship;

public record EnrollmentResponse(
        Long id,
        String schoolYear,
        String shift,
        String groupName,
        LocalDate enrollmentDate,
        StudentResponse student,
        GuardianResponse mother,
        GuardianResponse father,
        String previousSchooling,
        String previousInstitution,
        String responsibleType,
        String responsibleOther,
        boolean livesWithMother,
        boolean livesWithFather,
        String pickupType,
        String pickupOther,
        String illness,
        String allergies,
        boolean operated,
        String operationDetails,
        boolean psychologicalTreatment,
        String psychologicalDetails,
        String photoDataUrl,
        boolean commitmentAccepted,
        String commitmentSignatureName,
        LocalDate commitmentSignatureDate,
        String commitmentSignatureData,
        List<EnrollmentDocumentResponse> documents,
        Instant createdAt,
        Instant updatedAt) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        Guardian mother = guardian(enrollment, GuardianRelationship.MOTHER);
        Guardian father = guardian(enrollment, GuardianRelationship.FATHER);

        List<EnrollmentDocumentResponse> documents = enrollment.getDocuments().stream()
                .sorted(Comparator.comparing(document -> document.getDocumentType().ordinal()))
                .map(EnrollmentDocumentResponse::from)
                .toList();

        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getSchoolYear(),
                enrollment.getShift().name(),
                enrollment.getGroupName().name(),
                enrollment.getEnrollmentDate(),
                StudentResponse.from(enrollment.getStudent()),
                mother == null ? null : GuardianResponse.from(mother),
                father == null ? null : GuardianResponse.from(father),
                enrollment.getPreviousSchooling().name(),
                enrollment.getPreviousInstitution(),
                enrollment.getResponsibleType().name(),
                enrollment.getResponsibleOther(),
                enrollment.isLivesWithMother(),
                enrollment.isLivesWithFather(),
                enrollment.getPickupType().name(),
                enrollment.getPickupOther(),
                enrollment.getIllness(),
                enrollment.getAllergies(),
                enrollment.isOperated(),
                enrollment.getOperationDetails(),
                enrollment.isPsychologicalTreatment(),
                enrollment.getPsychologicalDetails(),
                enrollment.getPhotoDataUrl(),
                enrollment.isCommitmentAccepted(),
                enrollment.getCommitmentSignatureName(),
                enrollment.getCommitmentSignatureDate(),
                enrollment.getCommitmentSignatureData(),
                documents,
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt());
    }

    private static Guardian guardian(Enrollment enrollment, GuardianRelationship relationship) {
        return enrollment.getStudent().getGuardians().stream()
                .filter(guardian -> guardian.getRelationship() == relationship)
                .findFirst()
                .orElse(null);
    }
}
