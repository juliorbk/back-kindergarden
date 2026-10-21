package com.ueclca.backend.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.ueclca.backend.AbstractIntegrationTest;
import com.ueclca.backend.dtos.EventRequest;
import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.models.Event;
import com.ueclca.backend.models.EventCategory;
import com.ueclca.backend.models.EventKind;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.EventRepository;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.AuthService;
import com.ueclca.backend.services.EventService;

@AutoConfigureMockMvc
class EventApiIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ueclca.edu.ve";
    private static final String EDITOR_EMAIL = "editor@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String editorToken;

    @BeforeEach
    void setUp() {
        seedUser(ADMIN_EMAIL, Role.ADMIN);
        seedUser(EDITOR_EMAIL, Role.EDITOR);
        eventRepository.deleteAll();

        adminToken = authService.login(new LoginRequest(ADMIN_EMAIL, PASSWORD)).token();
        editorToken = authService.login(new LoginRequest(EDITOR_EMAIL, PASSWORD)).token();
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithEditorReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Festival cultural","description":"Baile y música",
                                 "location":"Patio central","category":"CULTURAL","allDay":true,
                                 "startsAt":"2026-11-20T00:00:00Z","endsAt":"2026-11-20T23:59:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category").value("CULTURAL"))
                .andExpect(jsonPath("$.allDay").value(true))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.publishedAt").doesNotExist());
    }

    @Test
    void createEphemerisStoresImageKindAndFeatured() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Día de la Tierra","description":"Cuidado del planeta",
                                 "category":"CULTURAL","kind":"EPHEMERIS",
                                 "imageUrl":"https://example.com/tierra.jpg","featured":true,
                                 "allDay":true,"startsAt":"2026-04-22T00:00:00Z","endsAt":"2026-04-22T23:59:59Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("EPHEMERIS"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/tierra.jpg"))
                .andExpect(jsonPath("$.featured").value(true));
    }

    @Test
    void createEphemerisWithoutImageReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Día de la Tierra","description":"Cuidado del planeta",
                                 "category":"CULTURAL","kind":"EPHEMERIS","allDay":true,
                                 "startsAt":"2026-04-22T00:00:00Z","endsAt":"2026-04-22T23:59:59Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutAllDayDefaultsToFalse() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Reunión breve","category":"MEETING",
                                 "startsAt":"2026-11-20T13:00:00Z","endsAt":"2026-11-20T14:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allDay").value(false));
    }

    @Test
    void createWithInvalidCategoryReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Evento","category":"FIESTA",
                                 "startsAt":"2026-11-20T00:00:00Z","endsAt":"2026-11-20T23:59:00Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithEndBeforeStartReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Evento","category":"MEETING",
                                 "startsAt":"2026-11-20T10:00:00Z","endsAt":"2026-11-20T09:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChangesFieldsButKeepsStatus() throws Exception {
        Event event = createEvent("Reunión de padres", "2026-11-10T13:00:00Z", "2026-11-10T15:00:00Z");

        mockMvc.perform(put("/api/v1/events/" + event.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Reunión de representantes","location":"Aula 3",
                                 "category":"MEETING","allDay":false,
                                 "startsAt":"2026-11-11T13:00:00Z","endsAt":"2026-11-11T14:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Reunión de representantes"))
                .andExpect(jsonPath("$.location").value("Aula 3"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(put("/api/v1/events/" + event.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Reunión","category":"MEETING",
                                 "startsAt":"2026-11-11T15:00:00Z","endsAt":"2026-11-11T14:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/events/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminListFiltersByStatusAndRange() throws Exception {
        Event inRange = createEvent("En rango", "2026-11-01T00:00:00Z", "2026-11-05T00:00:00Z");
        createEvent("Fuera de rango", "2026-12-01T00:00:00Z", "2026-12-03T00:00:00Z");
        publish(inRange);

        mockMvc.perform(get("/api/v1/events?status=PUBLISHED&from=2026-11-04T00:00:00Z&to=2026-11-20T00:00:00Z")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("En rango"));
    }

    @Test
    void publicListOnlyReturnsPublishedEventsInRangeOrderedByStart() throws Exception {
        Event one = createEvent("Evento uno", "2026-11-01T00:00:00Z", "2026-11-05T23:59:00Z");
        Event two = createEvent("Evento dos", "2026-11-10T00:00:00Z", "2026-11-12T23:59:00Z");
        Event three = createEvent("Evento tres", "2026-12-01T00:00:00Z", "2026-12-03T23:59:00Z");
        Event draft = createEvent("Evento borrador", "2026-11-02T00:00:00Z", "2026-11-03T00:00:00Z");
        publish(one);
        publish(two);
        publish(three);

        mockMvc.perform(get("/api/v1/public/events")
                        .param("from", "2026-11-04T00:00:00Z")
                        .param("to", "2026-11-11T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Evento uno"))
                .andExpect(jsonPath("$[1].title").value("Evento dos"));

        mockMvc.perform(get("/api/v1/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void publicListFiltersByKindAndFeatured() throws Exception {
        Event calendar = createEvent("Evento", "2026-11-01T00:00:00Z", "2026-11-02T00:00:00Z");
        Event ephemeris = eventService.create(new EventRequest(
                "Efeméride destacada", "Mensaje", null, EventCategory.CULTURAL,
                EventKind.EPHEMERIS, "https://example.com/foto.jpg", true, true,
                Instant.parse("2026-11-03T00:00:00Z"), Instant.parse("2026-11-03T23:59:59Z")),
                ADMIN_EMAIL);
        Event otherEphemeris = eventService.create(new EventRequest(
                "Otra efeméride", "Mensaje", null, EventCategory.CULTURAL,
                EventKind.EPHEMERIS, "https://example.com/otra.jpg", false, true,
                Instant.parse("2026-11-04T00:00:00Z"), Instant.parse("2026-11-04T23:59:59Z")),
                ADMIN_EMAIL);
        publish(calendar);
        publish(ephemeris);
        publish(otherEphemeris);

        mockMvc.perform(get("/api/v1/public/events")
                        .param("kind", "EPHEMERIS")
                        .param("featured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Efeméride destacada"));
    }

    @Test
    void unpublishRemovesEventFromPublicList() throws Exception {
        Event event = createEvent("Evento", "2026-11-01T00:00:00Z", "2026-11-02T00:00:00Z");
        publish(event);

        mockMvc.perform(get("/api/v1/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/v1/events/" + event.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DRAFT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deleteRemovesEvent() throws Exception {
        Event event = createEvent("Para eliminar", "2026-11-01T00:00:00Z", "2026-11-02T00:00:00Z");

        mockMvc.perform(delete("/api/v1/events/" + event.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/events/" + event.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    private Event createEvent(String title, String startsAt, String endsAt) {
        return eventService.create(new EventRequest(title, null, "Patio central",
                EventCategory.ACADEMIC, EventKind.CALENDAR, null, null, false,
                Instant.parse(startsAt), Instant.parse(endsAt)), ADMIN_EMAIL);
    }

    private void publish(Event event) {
        eventService.changeStatus(event.getId(), PublicationStatus.PUBLISHED);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void seedUser(String email, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .fullName("Usuario de prueba " + role)
                    .role(role)
                    .enabled(true)
                    .build());
        }
    }
}
