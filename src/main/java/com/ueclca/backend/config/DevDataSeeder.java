package com.ueclca.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final SeedProperties seedProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedProperties.enabled() || userRepository.existsByEmail(seedProperties.adminEmail())) {
            return;
        }

        userRepository.save(User.builder()
                .email(seedProperties.adminEmail())
                .passwordHash(passwordEncoder.encode(seedProperties.adminPassword()))
                .fullName(seedProperties.adminFullName())
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        log.info("Usuario admin de desarrollo creado: {}", seedProperties.adminEmail());
    }
}
