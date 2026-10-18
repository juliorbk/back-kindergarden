package com.ueclca.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.PostRepository;
import com.ueclca.backend.repositories.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled()) {
            return;
        }

        User admin = userRepository.findByEmail(seedProperties.adminEmail())
                .orElseGet(this::createAdminUser);
        seedPosts(admin.getEmail());
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
                "https://picsum.photos/seed/feria-ciencias/800/600"), authorEmail);
        postService.changeStatus(feria.getId(), PublicationStatus.PUBLISHED);

        Post inscripciones = postService.create(new PostRequest(
                "Inscripciones abiertas para el año escolar 2026-2027",
                "El proceso de inscripción estará abierto hasta el 15 de noviembre. "
                        + "Consulte los recaudos en la secretaría del colegio.",
                "https://picsum.photos/seed/inscripciones/800/600"), authorEmail);
        postService.changeStatus(inscripciones.getId(), PublicationStatus.PUBLISHED);

        postService.create(new PostRequest(
                "Jornada de puertas abiertas (borrador)",
                "Publicación de ejemplo en borrador, visible solo desde el panel administrativo.",
                "https://picsum.photos/seed/puertas-abiertas/800/600"), authorEmail);

        log.info("Publicaciones de ejemplo de desarrollo creadas");
    }
}
