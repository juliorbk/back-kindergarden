package com.ueclca.backend.dtos;

import java.time.LocalDate;
import java.util.List;

import com.ueclca.backend.models.GroupName;
import com.ueclca.backend.models.PickupType;
import com.ueclca.backend.models.PreviousSchooling;
import com.ueclca.backend.models.ResponsibleType;
import com.ueclca.backend.models.Shift;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnrollmentRequest(
        @NotBlank(message = "El año escolar es obligatorio")
        @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "El año escolar debe tener el formato 2026-2027")
        String schoolYear,

        @NotNull(message = "El turno es obligatorio")
        Shift shift,

        @NotNull(message = "El grupo es obligatorio")
        GroupName groupName,

        @NotNull(message = "La fecha de inscripción es obligatoria")
        LocalDate enrollmentDate,

        @NotNull(message = "Los datos del alumno son obligatorios")
        @Valid
        StudentRequest student,

        @NotNull(message = "La procedencia es obligatoria")
        PreviousSchooling previousSchooling,

        @Size(max = 200, message = "La institución no puede superar los 200 caracteres")
        String previousInstitution,

        @Valid
        GuardianRequest mother,

        @Valid
        GuardianRequest father,

        @NotNull(message = "Indique el responsable del alumno ante la institución")
        ResponsibleType responsibleType,

        @Size(max = 150, message = "El responsable no puede superar los 150 caracteres")
        String responsibleOther,

        Boolean livesWithMother,

        Boolean livesWithFather,

        @NotNull(message = "Indique quién retira al alumno")
        PickupType pickupType,

        @Size(max = 150, message = "La persona que retira al alumno no puede superar los 150 caracteres")
        String pickupOther,

        @Size(max = 1000, message = "La enfermedad no puede superar los 1000 caracteres")
        String illness,

        @Size(max = 1000, message = "La alergia no puede superar los 1000 caracteres")
        String allergies,

        Boolean operated,

        @Size(max = 500, message = "El detalle de la operación no puede superar los 500 caracteres")
        String operationDetails,

        Boolean psychologicalTreatment,

        @Size(max = 500, message = "El detalle del tratamiento no puede superar los 500 caracteres")
        String psychologicalDetails,

        @Size(max = 1500000, message = "La foto del alumno es demasiado grande")
        String photoDataUrl,

        @AssertTrue(message = "Debe aceptar el acta de compromiso")
        boolean commitmentAccepted,

        @NotBlank(message = "El nombre del representante que firma es obligatorio")
        @Size(max = 150, message = "El nombre de la firma no puede superar los 150 caracteres")
        String commitmentSignatureName,

        @NotNull(message = "La fecha de la firma es obligatoria")
        LocalDate commitmentSignatureDate,

        @Size(max = 500000, message = "La firma es demasiado grande")
        String commitmentSignatureData,

        @Size(max = 5, message = "Se admiten como máximo 5 documentos")
        List<@Valid EnrollmentDocumentRequest> documents) {
}
