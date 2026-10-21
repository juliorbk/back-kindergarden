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
class UserApiIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin.usuarios@ueclca.edu.ve";
    private static final String EDITOR_EMAIL = "editor.usuarios@ueclca.edu.ve";
    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUsers() {
        seedUser(ADMIN_EMAIL, "Administrador de usuarios", Role.ADMIN);
        seedUser(EDITOR_EMAIL, "Editor de usuarios", Role.EDITOR);
    }

    private void seedUser(String email, String fullName, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(buildUser(email, fullName, role));
        }
    }

    private User replaceUser(String email, String fullName, Role role) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        return userRepository.save(buildUser(email, fullName, role));
    }

    private User buildUser(String email, String fullName, Role role) {
        return User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build();
    }

    private String tokenFor(String email) {
        return authService.login(new LoginRequest(email, PASSWORD)).token();
    }

    private String adminToken() {
        return tokenFor(ADMIN_EMAIL);
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listWithEditorTokenReturnsForbiddenProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + tokenFor(EDITOR_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    void listWithAdminTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    void getByIdReturnsUser() throws Exception {
        Long id = userRepository.findByEmail(EDITOR_EMAIL).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/{id}", id)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EDITOR_EMAIL));
    }

    @Test
    void getByUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999_999L)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturnsCreated() throws Exception {
        userRepository.findByEmail("nuevo.usuario@ueclca.edu.ve").ifPresent(userRepository::delete);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nuevo.usuario@ueclca.edu.ve","password":"password123",
                                 "fullName":"Usuario Nuevo","role":"EDITOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nuevo.usuario@ueclca.edu.ve"))
                .andExpect(jsonPath("$.role").value("EDITOR"));
    }

    @Test
    void createWithDuplicateEmailReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123",
                                 "fullName":"Duplicado","role":"EDITOR"}
                                """.formatted(EDITOR_EMAIL)))
                .andExpect(status().isConflict());
    }

    @Test
    void createWithInvalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-es-correo","password":"corta","fullName":"","role":null}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        User victim = replaceUser("victima.update@ueclca.edu.ve", "Victima Update", Role.EDITOR);

        mockMvc.perform(put("/api/v1/users/{id}", victim.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Editor Renombrado","role":"EDITOR","enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Editor Renombrado"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updateSelfDisableReturnsBadRequest() throws Exception {
        Long id = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

        mockMvc.perform(put("/api/v1/users/{id}", id)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Administrador de usuarios","role":"ADMIN","enabled":false}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSelfRoleReturnsBadRequest() throws Exception {
        Long id = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

        mockMvc.perform(put("/api/v1/users/{id}", id)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Administrador de usuarios","role":"EDITOR","enabled":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordReturnsNoContentAndLoginWorksWithNewPassword() throws Exception {
        User target = replaceUser("victima.password@ueclca.edu.ve", "Victima Password", Role.EDITOR);

        mockMvc.perform(put("/api/v1/users/{id}/password", target.getId())
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"nuevaPassword123"}
                                """))
                .andExpect(status().isNoContent());

        assertNotNull(authService.login(new LoginRequest(target.getEmail(), "nuevaPassword123")));
    }

    @Test
    void resetPasswordForUnknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/users/{id}/password", 999_999L)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"nuevaPassword123"}
                                """))
                .andExpect(status().isNotFound());
    }
}
