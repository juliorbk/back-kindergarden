package com.ueclca.backend.dtos;

import java.time.Instant;

import com.ueclca.backend.models.MediaAsset;

public record MediaResponse(
        Long id,
        String url,
        String mimeType,
        long sizeBytes,
        Instant createdAt) {

    public static MediaResponse from(MediaAsset asset) {
        return new MediaResponse(
                asset.getId(),
                asset.getUrl(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getCreatedAt());
    }
}
