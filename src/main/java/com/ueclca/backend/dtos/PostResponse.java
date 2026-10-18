package com.ueclca.backend.dtos;

import java.time.Instant;

import com.ueclca.backend.models.Post;

public record PostResponse(
        Long id,
        String slug,
        String title,
        String caption,
        String imageUrl,
        String status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getCaption(),
                post.getImageUrl(),
                post.getStatus().name(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
