package com.ueclca.backend.dtos;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostMedia;

public record PostResponse(
        Long id,
        String slug,
        String title,
        String caption,
        String imageUrl,
        List<MediaResponse> media,
        List<PostChannelResponse> channels,
        String status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static PostResponse from(Post post) {
        List<MediaResponse> media = post.getMedia().stream()
                .sorted(Comparator.comparingInt(PostMedia::getPosition))
                .map(item -> MediaResponse.from(item.getMedia()))
                .toList();
        List<PostChannelResponse> channels = post.getChannels().stream()
                .sorted(Comparator.comparing(item -> item.getChannel().name()))
                .map(PostChannelResponse::from)
                .toList();

        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getCaption(),
                post.getImageUrl(),
                media,
                channels,
                post.getStatus().name(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
