package com.ueclca.backend.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.ueclca.backend.dtos.EventRequest;
import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.Event;
import com.ueclca.backend.models.EventCategory;
import com.ueclca.backend.models.EventKind;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.EventRepository;
import com.ueclca.backend.repositories.PostRepository;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.EventService;
import com.ueclca.backend.services.PostService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final SeedProperties seedProperties;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }
        validateSeedConfiguration();

        User admin = userRepository.findByEmail(seedProperties.adminEmail())
                .orElseGet(this::createAdminUser);
        seedPosts(admin.getEmail());
        seedEvents(admin.getEmail());
    }

    private void validateSeedConfiguration() {
        if (!StringUtils.hasText(seedProperties.adminEmail())) {
            throw new IllegalStateException("SEED_ADMIN_EMAIL es obligatorio cuando app.seed.enabled es true");
        }
        if (!StringUtils.hasText(seedProperties.adminFullName())) {
            throw new IllegalStateException("SEED_ADMIN_FULL_NAME es obligatorio cuando app.seed.enabled es true");
        }
        String password = seedProperties.adminPassword();
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException(
                    "SEED_ADMIN_PASSWORD es obligatorio y debe tener entre 8 y 72 caracteres");
        }
    }

    private User createAdminUser() {
        User admin = userRepository.save(User.builder()
                .email(seedProperties.adminEmail())
                .passwordHash(passwordEncoder.encode(seedProperties.adminPassword()))
                .fullName(seedProperties.adminFullName())
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        log.info("Usuario admin de desarrollo creado: {}", admin.getEmail());
        return admin;
    }

    private void seedPosts(String authorEmail) {
        if (postRepository.count() > 0) {
            return;
        }

        Post feria = postService.create(new PostRequest(
                "Feria de Ciencias 2026",
                "Los estudiantes presentaron sus proyectos de ciencias en el patio central. "
                        + "¡Gracias a las familias por acompañarnos!",
                "https://picsum.photos/seed/feria-ciencias/800/600",
                List.of(),
                Set.of(Channel.TELEGRAM, Channel.FACEBOOK)), authorEmail);
        postService.changeStatus(feria.getId(), PublicationStatus.PUBLISHED);

        Post inscripciones = postService.create(new PostRequest(
                "Inscripciones abiertas para el año escolar 2026-2027",
                "El proceso de inscripción estará abierto hasta el 15 de noviembre. "
                        + "Consulte los recaudos en la secretaría del colegio.",
                "https://picsum.photos/seed/inscripciones/800/600",
                List.of(),
                Set.of(Channel.TELEGRAM, Channel.FACEBOOK, Channel.INSTAGRAM)), authorEmail);
        postService.changeStatus(inscripciones.getId(), PublicationStatus.PUBLISHED);

        postService.create(new PostRequest(
                "Jornada de puertas abiertas (borrador)",
                "Publicación de ejemplo en borrador, visible solo desde el panel administrativo.",
                "https://picsum.photos/seed/puertas-abiertas/800/600",
                List.of(),
                Set.of(Channel.TELEGRAM)), authorEmail);

        log.info("Publicaciones de ejemplo de desarrollo creadas");
    }

    private void seedEvents(String authorEmail) {
        if (eventRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        createEvent(new EventRequest(
                "Reunión de padres y representantes",
                "Encuentro informativo sobre el avance del año escolar. Los esperamos en el auditorio.",
                "Auditorio principal",
                EventCategory.MEETING,
                EventKind.CALENDAR,
                null,
                true,
                false,
                atStart(today.plusDays(2)),
                atEnd(today.plusDays(2))), authorEmail);

        createEvent(new EventRequest(
                "Olimpiadas deportivas",
                "Jornada de juegos y competencias para todos los grados.",
                "Cancha central",
                EventCategory.SPORTS,
                EventKind.CALENDAR,
                null,
                false,
                true,
                atStart(today.plusDays(12)),
                atEnd(today.plusDays(12))), authorEmail);

        createEvent(new EventRequest(
                "Feriado escolar",
                "No hay actividades académicas.",
                null,
                EventCategory.HOLIDAY,
                EventKind.CALENDAR,
                null,
                false,
                true,
                atStart(today.plusDays(20)),
                atEnd(today.plusDays(20))), authorEmail);

        createEvent(new EventRequest(
                "Día de la Tierra",
                "Reflexionamos junto a nuestros alumnos sobre el cuidado del planeta y la naturaleza.",
                null,
                EventCategory.CULTURAL,
                EventKind.EPHEMERIS,
                "https://picsum.photos/seed/dia-de-la-tierra/800/600",
                false,
                true,
                atStart(today.plusDays(5)),
                atEnd(today.plusDays(5))), authorEmail);

        createEvent(new EventRequest(
                "Día de la Resistencia Indígena",
                "Honramos la memoria y el legado de nuestros pueblos originarios.",
                null,
                EventCategory.CULTURAL,
                EventKind.EPHEMERIS,
                "https://picsum.photos/seed/resistencia-indigena/800/600",
                false,
                true,
                atStart(today.plusDays(15)),
                atEnd(today.plusDays(15))), authorEmail);

        log.info("Eventos de ejemplo de desarrollo creados");
    }

    private void createEvent(EventRequest request, String authorEmail) {
        Event event = eventService.create(request, authorEmail);
        eventService.changeStatus(event.getId(), PublicationStatus.PUBLISHED);
    }

    private Instant atStart(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant atEnd(LocalDate date) {
        return date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }
}
