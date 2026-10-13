package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.ueclca.backend.AbstractIntegrationTest;
import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.dtos.LoginResponse;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.UserRepository;

class AuthServiceIT extends AbstractIntegrationTest {

    private static final String EMAIL = "admin@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void seedUser() {
        if (userRepository.findByEmail(EMAIL).isEmpty()) {
            userRepository.save(User.builder()
                    .email(EMAIL)
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .fullName("Administrador de prueba")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }
    }

    @Test
    void loginReturnsValidToken() {
        LoginResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertNotNull(response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(EMAIL, response.user().email());

        Jwt jwt = jwtDecoder.decode(response.token());
        assertEquals(EMAIL, jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
    }

    @Test
    void loginWithWrongPasswordFails() {
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest(EMAIL, "clave-incorrecta")));
    }
}
