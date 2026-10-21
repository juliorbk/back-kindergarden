package com.ueclca.backend.controllers;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ueclca.backend.dtos.EventRequest;
import com.ueclca.backend.dtos.EventResponse;
import com.ueclca.backend.dtos.PageResponse;
import com.ueclca.backend.dtos.StatusRequest;
import com.ueclca.backend.models.EventKind;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.services.EventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public PageResponse<EventResponse> list(
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) EventKind kind,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
        return PageResponse.from(
                eventService.search(status, kind, featured, from, to, pageable),
                EventResponse::from);
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return EventResponse.from(eventService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request, @AuthenticationPrincipal Jwt jwt) {
        return EventResponse.from(eventService.create(request, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return EventResponse.from(eventService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public EventResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return EventResponse.from(eventService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }
}
