package com.ueclca.backend.dtos;

import java.time.Instant;

import com.ueclca.backend.models.Event;

public record EventResponse(
        Long id,
        String title,
        String description,
        String location,
        String category,
        boolean allDay,
        Instant startsAt,
        Instant endsAt,
        String status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getCategory().name(),
                event.isAllDay(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getStatus().name(),
                event.getPublishedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
