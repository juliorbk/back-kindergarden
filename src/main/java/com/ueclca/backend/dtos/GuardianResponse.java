package com.ueclca.backend.dtos;

import java.time.LocalDate;

import com.ueclca.backend.models.Guardian;

public record GuardianResponse(
        Long id,
        String relationship,
        String fullName,
        String nationalId,
        String address,
        LocalDate birthDate,
        String occupation,
        boolean works,
        String employer,
        String email,
        String homePhone,
        String mobilePhone,
        String officePhone) {

    public static GuardianResponse from(Guardian guardian) {
        return new GuardianResponse(
                guardian.getId(),
                guardian.getRelationship().name(),
                guardian.getFullName(),
                guardian.getNationalId(),
                guardian.getAddress(),
                guardian.getBirthDate(),
                guardian.getOccupation(),
                guardian.isWorks(),
                guardian.getEmployer(),
                guardian.getEmail(),
                guardian.getHomePhone(),
                guardian.getMobilePhone(),
                guardian.getOfficePhone());
    }
}
