package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ueclca.backend.dtos.EventRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.models.Event;
import com.ueclca.backend.models.EventCategory;
import com.ueclca.backend.models.EventKind;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Instant START = Instant.parse("2026-11-20T13:00:00Z");
    private static final Instant END = Instant.parse("2026-11-20T15:00:00Z");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventService eventService;

    @Test
    void createRejectsEndBeforeStart() {
        EventRequest request = new EventRequest("Evento", null, null, EventCategory.MEETING,
                null, null, null, false, START, START.minusSeconds(3600));

        assertThrows(BadRequestException.class, () -> eventService.create(request, "admin@ueclca.edu.ve"));

        verifyNoInteractions(eventRepository, userService);
    }

    @Test
    void createDefaultsAllDayToFalseAndNormalizesOptionalFields() {
        User creator = User.builder()
                .email("admin@ueclca.edu.ve")
                .fullName("Administrador")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        when(userService.getByEmail("admin@ueclca.edu.ve")).thenReturn(creator);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event event = eventService.create(new EventRequest(
                "  Evento  ", "   ", "  Patio central  ", EventCategory.CULTURAL,
                null, null, null, null, START, END),
                "admin@ueclca.edu.ve");

        assertEquals("Evento", event.getTitle());
        assertNull(event.getDescription());
        assertEquals("Patio central", event.getLocation());
        assertEquals(EventKind.CALENDAR, event.getKind());
        assertNull(event.getImageUrl());
        assertFalse(event.isFeatured());
        assertFalse(event.isAllDay());
        assertEquals(PublicationStatus.DRAFT, event.getStatus());
    }

    @Test
    void createEphemerisRequiresImageAndDescription() {
        User creator = User.builder()
                .email("admin@ueclca.edu.ve")
                .fullName("Administrador")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        when(userService.getByEmail("admin@ueclca.edu.ve")).thenReturn(creator);

        EventRequest withoutImage = new EventRequest(
                "Día de la Tierra", "Mensaje", null, EventCategory.CULTURAL,
                EventKind.EPHEMERIS, null, null, true, START, END);
        assertThrows(BadRequestException.class,
                () -> eventService.create(withoutImage, "admin@ueclca.edu.ve"));

        EventRequest withoutDescription = new EventRequest(
                "Día de la Tierra", "  ", null, EventCategory.CULTURAL,
                EventKind.EPHEMERIS, "https://example.com/tierra.jpg", null, true, START, END);
        assertThrows(BadRequestException.class,
                () -> eventService.create(withoutDescription, "admin@ueclca.edu.ve"));
    }

    @Test
    void createEphemerisWithImageAndDescriptionSucceeds() {
        User creator = User.builder()
                .email("admin@ueclca.edu.ve")
                .fullName("Administrador")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        when(userService.getByEmail("admin@ueclca.edu.ve")).thenReturn(creator);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event event = eventService.create(new EventRequest(
                "Día de la Tierra", "  Mensaje  ", null, EventCategory.CULTURAL,
                EventKind.EPHEMERIS, "https://example.com/tierra.jpg", true, true, START, END),
                "admin@ueclca.edu.ve");

        assertEquals(EventKind.EPHEMERIS, event.getKind());
        assertEquals("https://example.com/tierra.jpg", event.getImageUrl());
        assertTrue(event.isFeatured());
        assertEquals("Mensaje", event.getDescription());
    }

    @Test
    void changeStatusSetsPublishedAtOnlyOnce() {
        Event event = Event.builder()
                .title("Evento")
                .status(PublicationStatus.DRAFT)
                .build();
        when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(event));

        eventService.changeStatus(1L, PublicationStatus.PUBLISHED);
        Instant firstPublishedAt = event.getPublishedAt();
        assertNotNull(firstPublishedAt);

        eventService.changeStatus(1L, PublicationStatus.DRAFT);
        eventService.changeStatus(1L, PublicationStatus.PUBLISHED);

        assertEquals(firstPublishedAt, event.getPublishedAt());
    }

    @Test
    void searchPublishedSortsByStartAscending() {
        Event event = Event.builder()
                .title("Evento")
                .status(PublicationStatus.PUBLISHED)
                .build();
        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        List<Event> events = eventService.searchPublished(null, null);

        assertEquals(List.of(event), events);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(ArgumentMatchers.<Specification<Event>>any(), captor.capture());

        Sort.Order order = captor.getValue().getSort().getOrderFor("startsAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }
}
