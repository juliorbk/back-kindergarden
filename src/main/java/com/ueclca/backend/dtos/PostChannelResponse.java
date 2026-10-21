package com.ueclca.backend.dtos;

import java.time.Instant;

import com.ueclca.backend.models.PostChannel;

public record PostChannelResponse(
        String channel,
        String status,
        String externalId,
        String errorMessage,
        Instant sentAt) {

    public static PostChannelResponse from(PostChannel postChannel) {
        return new PostChannelResponse(
                postChannel.getChannel().name(),
                postChannel.getStatus().name(),
                postChannel.getExternalId(),
                postChannel.getErrorMessage(),
                postChannel.getSentAt());
    }
}
