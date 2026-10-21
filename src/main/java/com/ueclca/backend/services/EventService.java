package com.ueclca.backend.services;

import com.ueclca.backend.dtos.EventRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Event;
import com.ueclca.backend.models.EventKind;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.EventRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;

    public EventService(EventRepository eventRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.userService = userService;
    }

    public Page<Event> search(
        PublicationStatus status,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        return search(status, null, null, from, to, pageable);
    }

    public Page<Event> search(
        PublicationStatus status,
        EventKind kind,
        Boolean featured,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        return eventRepository.findAll(filter(status, kind, featured, from, to), pageable);
    }

    public Event getById(Long id) {
        return eventRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
    }

    @Transactional
    public Event create(EventRequest request, String creatorEmail) {
        validatePeriod(request.startsAt(), request.endsAt());
        User creator = userService.getByEmail(creatorEmail);

        String description = normalizeNullable(request.description());
        String imageUrl = normalizeNullable(request.imageUrl());
        EventKind kind = request.kind() == null ? EventKind.CALENDAR : request.kind();
        validateEphemeris(kind, imageUrl, description);

        Event event = Event.builder()
            .title(request.title().trim())
            .description(description)
            .location(normalizeNullable(request.location()))
            .category(request.category())
            .kind(kind)
            .imageUrl(imageUrl)
            .featured(Boolean.TRUE.equals(request.featured()))
            .allDay(Boolean.TRUE.equals(request.allDay()))
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .status(PublicationStatus.DRAFT)
            .createdBy(creator)
            .build();

        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, EventRequest request) {
        validatePeriod(request.startsAt(), request.endsAt());

        Event event = getById(id);
        String description = normalizeNullable(request.description());
        String imageUrl = normalizeNullable(request.imageUrl());
        EventKind kind = request.kind() == null ? EventKind.CALENDAR : request.kind();
        validateEphemeris(kind, imageUrl, description);

        event.setTitle(request.title().trim());
        event.setDescription(description);
        event.setLocation(normalizeNullable(request.location()));
        event.setCategory(request.category());
        event.setKind(kind);
        event.setImageUrl(imageUrl);
        event.setFeatured(Boolean.TRUE.equals(request.featured()));
        event.setAllDay(Boolean.TRUE.equals(request.allDay()));
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        return event;
    }

    @Transactional
    public Event changeStatus(Long id, PublicationStatus status) {
        Event event = getById(id);
        if (event.getStatus() != status) {
            if (status == PublicationStatus.PUBLISHED && event.getPublishedAt() == null) {
                event.setPublishedAt(Instant.now());
            }
            event.setStatus(status);
        }
        return event;
    }

    @Transactional
    public void delete(Long id) {
        eventRepository.delete(getById(id));
    }

    public List<Event> searchPublished(Instant from, Instant to) {
        return searchPublished(from, to, null, null);
    }

    public List<Event> searchPublished(Instant from, Instant to, EventKind kind, Boolean featured) {
        Pageable sortedByStartsAt = Pageable.unpaged(Sort.by(Sort.Direction.ASC, "startsAt"));
        return eventRepository
            .findAll(filter(PublicationStatus.PUBLISHED, kind, featured, from, to), sortedByStartsAt)
            .getContent();
    }

    private Specification<Event> filter(
        PublicationStatus status,
        EventKind kind,
        Boolean featured,
        Instant from,
        Instant to
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (kind != null) {
                predicates.add(cb.equal(root.get("kind"), kind));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endsAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startsAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validatePeriod(Instant startsAt, Instant endsAt) {
        if (endsAt.isBefore(startsAt)) {
            throw new BadRequestException(
                "La fecha de fin no puede ser anterior a la fecha de inicio"
            );
        }
    }

    private void validateEphemeris(EventKind kind, String imageUrl, String description) {
        if (kind != EventKind.EPHEMERIS) {
            return;
        }
        if (imageUrl == null) {
            throw new BadRequestException("La efeméride debe incluir una imagen");
        }
        if (description == null) {
            throw new BadRequestException("La efeméride debe incluir un mensaje");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
