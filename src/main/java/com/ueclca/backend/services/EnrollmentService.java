package com.ueclca.backend.services;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ueclca.backend.config.EnrollmentProperties;
import com.ueclca.backend.dtos.EnrollmentDocumentRequest;
import com.ueclca.backend.dtos.EnrollmentDocumentUploadResponse;
import com.ueclca.backend.dtos.EnrollmentRequest;
import com.ueclca.backend.dtos.EnrollmentResponse;
import com.ueclca.backend.dtos.EnrollmentSummaryResponse;
import com.ueclca.backend.dtos.GuardianRequest;
import com.ueclca.backend.dtos.StudentRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ConflictException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Enrollment;
import com.ueclca.backend.models.EnrollmentDocument;
import com.ueclca.backend.models.EnrollmentDocumentType;
import com.ueclca.backend.models.GroupName;
import com.ueclca.backend.models.Guardian;
import com.ueclca.backend.models.GuardianRelationship;
import com.ueclca.backend.models.PickupType;
import com.ueclca.backend.models.PreviousSchooling;
import com.ueclca.backend.models.ResponsibleType;
import com.ueclca.backend.models.Student;
import com.ueclca.backend.repositories.EnrollmentRepository;
import com.ueclca.backend.repositories.StudentRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private static final Pattern SCHOOL_YEAR_PATTERN = Pattern.compile("^(\\d{4})-(\\d{4})$");
    private static final Pattern STORAGE_KEY_PATTERN = Pattern.compile(
            "^[0-9a-fA-F-]{36}\\.(pdf|jpg|jpeg|png|webp)$");
    private static final Set<String> ALLOWED_DOCUMENT_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp");

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentDocumentStorage documentStorage;
    private final EnrollmentProperties enrollmentProperties;

    public Page<EnrollmentSummaryResponse> search(
            String schoolYear,
            GroupName groupName,
            String query,
            Pageable pageable) {
        return enrollmentRepository
                .findAll(filter(schoolYear, groupName, query), pageable)
                .map(EnrollmentSummaryResponse::from);
    }

    public EnrollmentResponse get(Long id) {
        return EnrollmentResponse.from(getEntity(id));
    }

    @Transactional
    public EnrollmentResponse createPublic(EnrollmentRequest request) {
        validate(request);
        Student student = resolveStudentForCreate(request.student(), request.schoolYear());
        applyStudent(student, request.student());
        applyGuardians(student, request.mother(), request.father());
        Student savedStudent = studentRepository.save(student);

        Enrollment enrollment = Enrollment.builder()
                .student(savedStudent)
                .build();
        applyEnrollment(enrollment, request);
        syncDocuments(enrollment, request.documents());

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Inscripción {} registrada para {} ({}, grupo {})",
                saved.getId(), savedStudent.getLastNames(), saved.getSchoolYear(), saved.getGroupName());
        return EnrollmentResponse.from(saved);
    }

    @Transactional
    public EnrollmentResponse update(Long id, EnrollmentRequest request) {
        validate(request);
        Enrollment enrollment = getEntity(id);
        Student student = enrollment.getStudent();

        applyStudent(student, request.student());
        applyGuardians(student, request.mother(), request.father());
        studentRepository.save(student);

        applyEnrollment(enrollment, request);
        syncDocuments(enrollment, request.documents());

        log.info("Inscripción {} actualizada para el año {}", id, enrollment.getSchoolYear());
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public void delete(Long id) {
        Enrollment enrollment = getEntity(id);
        Long studentId = enrollment.getStudent().getId();
        enrollmentRepository.delete(enrollment);
        enrollmentRepository.flush();

        if (enrollmentRepository.countByStudentId(studentId) == 0) {
            studentRepository.deleteById(studentId);
        }
        log.info("Inscripción {} eliminada", id);
    }

    public EnrollmentDocumentUploadResponse storeDocument(MultipartFile file) {
        EnrollmentDocumentStorage.StoredFile stored = documentStorage.store(file);
        return new EnrollmentDocumentUploadResponse(
                stored.storageKey(),
                stored.fileName(),
                stored.mimeType(),
                stored.sizeBytes());
    }

    public DocumentDownload loadDocument(Long enrollmentId, EnrollmentDocumentType documentType) {
        Enrollment enrollment = getEntity(enrollmentId);
        EnrollmentDocument document = enrollment.getDocuments().stream()
                .filter(item -> item.getDocumentType() == documentType)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La inscripción no tiene el documento solicitado"));

        if (document.getStorageKey() == null || document.getStorageKey().isBlank()) {
            throw new ResourceNotFoundException("El documento no tiene un archivo adjunto");
        }

        return new DocumentDownload(
                documentStorage.load(document.getStorageKey()),
                document.getFileName(),
                document.getMimeType());
    }

    private Enrollment getEntity(Long id) {
        return enrollmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada"));
    }

    private Student resolveStudentForCreate(StudentRequest request, String schoolYear) {
        Student student = studentRepository
                .findFirstByFirstNamesIgnoreCaseAndLastNamesIgnoreCaseAndBirthDate(
                        request.firstNames().trim(),
                        request.lastNames().trim(),
                        request.birthDate())
                .orElseGet(Student::new);

        if (student.getId() != null
                && enrollmentRepository.existsByStudentIdAndSchoolYear(student.getId(), schoolYear)) {
            throw new ConflictException(
                    "El alumno ya tiene una inscripción registrada para el año escolar " + schoolYear);
        }
        return student;
    }

    private void applyStudent(Student student, StudentRequest request) {
        student.setFirstNames(request.firstNames().trim());
        student.setLastNames(request.lastNames().trim());
        student.setBirthDate(request.birthDate());
        student.setBirthPlace(normalizeNullable(request.birthPlace()));
        student.setNationality(request.nationality().trim());
        student.setSex(request.sex());
        student.setHeightCm(request.heightCm());
        student.setWeightKg(request.weightKg());
        student.setSchoolRegistryNumber(normalizeNullable(request.schoolRegistryNumber()));
        student.setPhone(normalizeNullable(request.phone()));
        student.setAddress(request.address().trim());
    }

    private void applyGuardians(Student student, GuardianRequest mother, GuardianRequest father) {
        applyGuardian(student, GuardianRelationship.MOTHER, mother);
        applyGuardian(student, GuardianRelationship.FATHER, father);
    }

    private void applyGuardian(Student student, GuardianRelationship relationship, GuardianRequest request) {
        Guardian existing = student.getGuardians().stream()
                .filter(guardian -> guardian.getRelationship() == relationship)
                .findFirst()
                .orElse(null);

        if (request == null || !hasGuardianData(request)) {
            if (existing != null) {
                student.getGuardians().remove(existing);
            }
            return;
        }

        Guardian guardian = existing;
        if (guardian == null) {
            guardian = Guardian.builder()
                    .student(student)
                    .relationship(relationship)
                    .build();
            student.getGuardians().add(guardian);
        }

        guardian.setFullName(request.fullName().trim());
        guardian.setNationalId(normalizeCedula(request.nationalId()));
        guardian.setAddress(normalizeNullable(request.address()));
        guardian.setBirthDate(request.birthDate());
        guardian.setOccupation(normalizeNullable(request.occupation()));
        guardian.setWorks(Boolean.TRUE.equals(request.works()));
        guardian.setEmployer(normalizeNullable(request.employer()));
        guardian.setEmail(normalizeNullable(request.email()));
        guardian.setHomePhone(normalizeNullable(request.homePhone()));
        guardian.setMobilePhone(normalizeNullable(request.mobilePhone()));
        guardian.setOfficePhone(normalizeNullable(request.officePhone()));
    }

    private void applyEnrollment(Enrollment enrollment, EnrollmentRequest request) {
        enrollment.setSchoolYear(request.schoolYear().trim());
        enrollment.setShift(request.shift());
        enrollment.setGroupName(request.groupName());
        enrollment.setEnrollmentDate(request.enrollmentDate());
        enrollment.setPreviousSchooling(request.previousSchooling());
        enrollment.setPreviousInstitution(normalizeNullable(request.previousInstitution()));
        enrollment.setResponsibleType(request.responsibleType());
        enrollment.setResponsibleOther(
                request.responsibleType() == ResponsibleType.OTHER
                        ? normalizeNullable(request.responsibleOther())
                        : null);
        enrollment.setLivesWithMother(Boolean.TRUE.equals(request.livesWithMother()));
        enrollment.setLivesWithFather(Boolean.TRUE.equals(request.livesWithFather()));
        enrollment.setPickupType(request.pickupType());
        enrollment.setPickupOther(
                request.pickupType() == PickupType.OTHER
                        ? normalizeNullable(request.pickupOther())
                        : null);
        enrollment.setIllness(normalizeNullable(request.illness()));
        enrollment.setAllergies(normalizeNullable(request.allergies()));
        enrollment.setOperated(Boolean.TRUE.equals(request.operated()));
        enrollment.setOperationDetails(
                Boolean.TRUE.equals(request.operated())
                        ? normalizeNullable(request.operationDetails())
                        : null);
        enrollment.setPsychologicalTreatment(Boolean.TRUE.equals(request.psychologicalTreatment()));
        enrollment.setPsychologicalDetails(
                Boolean.TRUE.equals(request.psychologicalTreatment())
                        ? normalizeNullable(request.psychologicalDetails())
                        : null);
        enrollment.setPhotoDataUrl(normalizeNullable(request.photoDataUrl()));
        enrollment.setCommitmentAccepted(request.commitmentAccepted());
        enrollment.setCommitmentSignatureName(request.commitmentSignatureName().trim());
        enrollment.setCommitmentSignatureDate(request.commitmentSignatureDate());
        enrollment.setCommitmentSignatureData(normalizeNullable(request.commitmentSignatureData()));
    }

    private void syncDocuments(Enrollment enrollment, List<EnrollmentDocumentRequest> requests) {
        if (requests == null) {
            return;
        }

        Map<EnrollmentDocumentType, EnrollmentDocument> existing = enrollment.getDocuments().stream()
                .collect(Collectors.toMap(EnrollmentDocument::getDocumentType, Function.identity()));

        Set<EnrollmentDocumentType> incoming = new HashSet<>();
        for (EnrollmentDocumentRequest request : requests) {
            if (request == null) {
                continue;
            }
            if (!incoming.add(request.documentType())) {
                throw new BadRequestException(
                        "El documento " + request.documentType() + " está repetido");
            }
        }

        enrollment.getDocuments().removeIf(document -> !incoming.contains(document.getDocumentType()));

        for (EnrollmentDocumentRequest request : requests) {
            if (request == null) {
                continue;
            }
            EnrollmentDocument document = existing.get(request.documentType());
            if (document == null || !incoming.contains(document.getDocumentType())) {
                document = EnrollmentDocument.builder()
                        .enrollment(enrollment)
                        .documentType(request.documentType())
                        .build();
                enrollment.getDocuments().add(document);
            }

            String storageKey = normalizeNullable(request.storageKey());
            document.setPresented(request.presented());
            document.setStorageKey(storageKey);
            document.setFileName(storageKey == null ? null : normalizeNullable(request.fileName()));
            document.setMimeType(storageKey == null ? null : normalizeNullable(request.mimeType()));
            document.setSizeBytes(storageKey == null ? null : request.sizeBytes());
        }
    }

    private Specification<Enrollment> filter(String schoolYear, GroupName groupName, String query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String year = normalizeNullable(schoolYear);
            if (year != null) {
                predicates.add(cb.equal(root.get("schoolYear"), year));
            }
            if (groupName != null) {
                predicates.add(cb.equal(root.get("groupName"), groupName));
            }
            String search = normalizeNullable(query);
            if (search != null) {
                String like = "%" + search.toLowerCase() + "%";
                Join<Enrollment, Student> student = root.join("student");
                predicates.add(cb.or(
                        cb.like(cb.lower(student.get("firstNames")), like),
                        cb.like(cb.lower(student.get("lastNames")), like),
                        cb.like(cb.lower(student.get("schoolRegistryNumber")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validate(EnrollmentRequest request) {
        int startYear = validateSchoolYear(request.schoolYear());
        validateAgeForGroup(request.student().birthDate(), request.groupName(), startYear);
        validateEnrollmentDate(request.enrollmentDate(), startYear);
        validateGuardians(request.mother(), request.father());
        validatePreviousSchooling(request);
        validateConditionalFields(request);
        validateImage(request.photoDataUrl(), "La foto del alumno");
        validateImage(request.commitmentSignatureData(), "La firma del representante");
        validateDocuments(request.documents());
    }

    private int validateSchoolYear(String schoolYear) {
        Matcher matcher = SCHOOL_YEAR_PATTERN.matcher(schoolYear.trim());
        if (!matcher.matches()) {
            throw new BadRequestException("El año escolar debe tener el formato 2026-2027");
        }
        int start = Integer.parseInt(matcher.group(1));
        int end = Integer.parseInt(matcher.group(2));
        if (end != start + 1) {
            throw new BadRequestException("El año escolar debe ser consecutivo (ej. 2026-2027)");
        }
        int current = LocalDate.now().getYear();
        if (start < current - 2 || start > current + 3) {
            throw new BadRequestException(
                    "El año escolar debe estar entre " + (current - 2) + " y " + (current + 3));
        }
        return start;
    }

    private void validateAgeForGroup(LocalDate birthDate, GroupName groupName, int startYear) {
        LocalDate reference = LocalDate.of(startYear, 9, 1);
        if (birthDate.isAfter(reference)) {
            throw new BadRequestException(
                    "La fecha de nacimiento no puede ser posterior al inicio del año escolar");
        }
        int age = Period.between(birthDate, reference).getYears();
        boolean valid = switch (groupName) {
            case MATERNAL -> age < 4;
            case GROUP_I -> age >= 3 && age < 5;
            case GROUP_II -> age >= 4 && age < 6;
            case GROUP_III -> age >= 5 && age < 7;
        };
        if (!valid) {
            throw new BadRequestException(
                    "La edad del alumno (" + age + " años) no corresponde al grupo "
                            + groupName + ". Verifica la fecha de nacimiento o el grupo.");
        }
    }

    private void validateEnrollmentDate(LocalDate enrollmentDate, int startYear) {
        LocalDate min = LocalDate.of(startYear, 8, 1);
        LocalDate max = LocalDate.of(startYear + 1, 10, 31);
        if (enrollmentDate.isBefore(min) || enrollmentDate.isAfter(max)) {
            throw new BadRequestException(
                    "La fecha de inscripción debe estar dentro del año escolar");
        }
    }

    private void validateGuardians(GuardianRequest mother, GuardianRequest father) {
        validateGuardian("madre", mother);
        validateGuardian("padre", father);
        if (!isCompleteGuardian(mother) && !isCompleteGuardian(father)) {
            throw new BadRequestException(
                    "Registra al menos un representante (madre o padre) con nombre, cédula y teléfono");
        }
    }

    private void validateGuardian(String label, GuardianRequest guardian) {
        if (guardian == null || !hasGuardianData(guardian)) {
            return;
        }
        if (!hasText(guardian.fullName())) {
            throw new BadRequestException("El nombre y apellido de la " + label + " es obligatorio");
        }
        if (!hasText(guardian.nationalId())) {
            throw new BadRequestException("La cédula de la " + label + " es obligatoria");
        }
        if (!hasAnyPhone(guardian)) {
            throw new BadRequestException(
                    "Registra al menos un teléfono de la " + label);
        }
    }

    private void validatePreviousSchooling(EnrollmentRequest request) {
        if (request.previousSchooling() != PreviousSchooling.HOME
                && request.previousSchooling() != PreviousSchooling.SAME_SCHOOL
                && !hasText(request.previousInstitution())) {
            throw new BadRequestException(
                    "Indica la institución donde estudió el alumno");
        }
    }

    private void validateConditionalFields(EnrollmentRequest request) {
        if (request.responsibleType() == ResponsibleType.OTHER
                && !hasText(request.responsibleOther())) {
            throw new BadRequestException("Especifica quién es el responsable del alumno");
        }
        if (request.pickupType() == PickupType.OTHER && !hasText(request.pickupOther())) {
            throw new BadRequestException("Especifica quién retira al alumno");
        }
        if (Boolean.TRUE.equals(request.operated()) && !hasText(request.operationDetails())) {
            throw new BadRequestException("Indica de qué fue operado el alumno");
        }
        if (Boolean.TRUE.equals(request.psychologicalTreatment())
                && !hasText(request.psychologicalDetails())) {
            throw new BadRequestException("Explica el tratamiento psicológico del alumno");
        }
    }

    private void validateImage(String dataUrl, String label) {
        String value = normalizeNullable(dataUrl);
        if (value == null) {
            return;
        }
        if (!value.startsWith("data:image/jpeg;base64,")
                && !value.startsWith("data:image/png;base64,")
                && !value.startsWith("data:image/webp;base64,")) {
            throw new BadRequestException(label + " debe ser una imagen JPG, PNG o WebP");
        }
    }

    private void validateDocuments(List<EnrollmentDocumentRequest> documents) {
        if (documents == null) {
            return;
        }
        Set<EnrollmentDocumentType> types = new HashSet<>();
        for (EnrollmentDocumentRequest document : documents) {
            if (document == null) {
                continue;
            }
            if (!types.add(document.documentType())) {
                throw new BadRequestException(
                        "El documento " + document.documentType() + " está repetido");
            }
            String storageKey = normalizeNullable(document.storageKey());
            if (storageKey == null) {
                continue;
            }
            if (!STORAGE_KEY_PATTERN.matcher(storageKey).matches()
                    || !documentStorage.exists(storageKey)) {
                throw new BadRequestException("El archivo adjunto no existe o no es válido");
            }
            if (!hasText(document.fileName())) {
                throw new BadRequestException("El archivo adjunto no tiene nombre");
            }
            if (document.mimeType() == null
                    || !ALLOWED_DOCUMENT_MIME_TYPES.contains(document.mimeType())) {
                throw new BadRequestException("El tipo del archivo adjunto no es válido");
            }
            long size = document.sizeBytes() == null ? 0 : document.sizeBytes();
            if (size <= 0 || size > enrollmentProperties.maxDocumentSizeBytes()) {
                throw new BadRequestException("El tamaño del archivo adjunto no es válido");
            }
        }
    }

    private boolean hasGuardianData(GuardianRequest guardian) {
        return hasText(guardian.fullName())
                || hasText(guardian.nationalId())
                || hasText(guardian.address())
                || guardian.birthDate() != null
                || hasText(guardian.occupation())
                || Boolean.TRUE.equals(guardian.works())
                || hasText(guardian.employer())
                || hasText(guardian.email())
                || hasAnyPhone(guardian);
    }

    private boolean isCompleteGuardian(GuardianRequest guardian) {
        return guardian != null
                && hasText(guardian.fullName())
                && hasText(guardian.nationalId())
                && hasAnyPhone(guardian);
    }

    private boolean hasAnyPhone(GuardianRequest guardian) {
        return hasText(guardian.homePhone())
                || hasText(guardian.mobilePhone())
                || hasText(guardian.officePhone());
    }

    private String normalizeCedula(String value) {
        String trimmed = value.trim().toUpperCase();
        if (trimmed.matches("^[VE]\\d{6,9}$")) {
            return trimmed.charAt(0) + "-" + trimmed.substring(1);
        }
        return trimmed;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record DocumentDownload(Resource resource, String fileName, String mimeType) {
    }
}
