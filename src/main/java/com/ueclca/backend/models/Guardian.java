package com.ueclca.backend.models;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "guardians")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuardianRelationship relationship;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(length = 500)
    private String address;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 150)
    private String occupation;

    @Column(nullable = false)
    private boolean works;

    @Column(length = 150)
    private String employer;

    @Column(length = 255)
    private String email;

    @Column(name = "home_phone", length = 30)
    private String homePhone;

    @Column(name = "mobile_phone", length = 30)
    private String mobilePhone;

    @Column(name = "office_phone", length = 30)
    private String officePhone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
