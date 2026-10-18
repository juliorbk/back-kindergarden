package com.ueclca.backend.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PublicationStatus;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    Page<Post> findByStatus(PublicationStatus status, Pageable pageable);

    boolean existsBySlug(String slug);
}
