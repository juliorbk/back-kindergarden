package com.ueclca.backend.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "school_year", nullable = false, length = 9)
    private String schoolYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_name", nullable = false, length = 20)
    private GroupName groupName;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_schooling", nullable = false, length = 30)
    private PreviousSchooling previousSchooling;

    @Column(name = "previous_institution", length = 200)
    private String previousInstitution;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_type", nullable = false, length = 20)
    private ResponsibleType responsibleType;

    @Column(name = "responsible_other", length = 150)
    private String responsibleOther;

    @Builder.Default
    @Column(name = "lives_with_mother", nullable = false)
    private boolean livesWithMother = false;

    @Builder.Default
    @Column(name = "lives_with_father", nullable = false)
    private boolean livesWithFather = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "pickup_type", nullable = false, length = 20)
    private PickupType pickupType;

    @Column(name = "pickup_other", length = 150)
    private String pickupOther;

    @Column(length = 1000)
    private String illness;

    @Column(length = 1000)
    private String allergies;

    @Builder.Default
    @Column(nullable = false)
    private boolean operated = false;

    @Column(name = "operation_details", length = 500)
    private String operationDetails;

    @Builder.Default
    @Column(name = "psychological_treatment", nullable = false)
    private boolean psychologicalTreatment = false;

    @Column(name = "psychological_details", length = 500)
    private String psychologicalDetails;

    @Column(name = "photo_data_url", columnDefinition = "TEXT")
    private String photoDataUrl;

    @Column(name = "commitment_accepted", nullable = false)
    private boolean commitmentAccepted;

    @Column(name = "commitment_signature_name", nullable = false, length = 150)
    private String commitmentSignatureName;

    @Column(name = "commitment_signature_date", nullable = false)
    private LocalDate commitmentSignatureDate;

    @Column(name = "commitment_signature_data", columnDefinition = "TEXT")
    private String commitmentSignatureData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)
    private List<EnrollmentDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
