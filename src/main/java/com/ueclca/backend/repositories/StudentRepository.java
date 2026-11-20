package com.ueclca.backend.repositories;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ueclca.backend.models.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findFirstByFirstNamesIgnoreCaseAndLastNamesIgnoreCaseAndBirthDate(
            String firstNames,
            String lastNames,
            LocalDate birthDate);
}
