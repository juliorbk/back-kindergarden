package com.ueclca.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.ueclca.backend.AbstractIntegrationTest;
import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.AuthService;

@AutoConfigureMockMvc
class AuthApiIT extends AbstractIntegrationTest {

    private static final String EMAIL = "admin@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void meWithoutTokenReturnsUnauthorizedProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autorizado"));
    }

    @Test
    void meWithTokenReturnsOk() throws Exception {
        String token = authService.login(new LoginRequest(EMAIL, PASSWORD)).token();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithValidCredentialsReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void loginWithInvalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-es-un-correo","password":"corta"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"clave-incorrecta"}
                                """.formatted(EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordWithWrongCurrentPasswordReturnsBadRequest() throws Exception {
        String token = authService.login(new LoginRequest(EMAIL, PASSWORD)).token();

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"incorrecta","newPassword":"nuevaPassword123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordReturnsNoContentAndLoginWorksWithNewPassword() throws Exception {
        String email = "cambio.password@ueclca.edu.ve";
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName("Cambio de contraseña")
                .role(Role.EDITOR)
                .enabled(true)
                .build());

        String token = authService.login(new LoginRequest(email, PASSWORD)).token();

        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"nuevaPassword123"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        assertNotNull(authService.login(new LoginRequest(email, "nuevaPassword123")));
    }
}
