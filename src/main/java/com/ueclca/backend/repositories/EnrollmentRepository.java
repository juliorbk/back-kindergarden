package com.ueclca.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ueclca.backend.models.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>, JpaSpecificationExecutor<Enrollment> {

    boolean existsByStudentIdAndSchoolYear(Long studentId, String schoolYear);

    long countByStudentId(Long studentId);
}
