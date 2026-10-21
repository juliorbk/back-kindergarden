package com.ueclca.backend.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.ueclca.backend.AbstractIntegrationTest;
import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.AuthService;

@AutoConfigureMockMvc
class MediaApiIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ueclca.edu.ve";
    private static final String EDITOR_EMAIL = "editor@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

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

        adminToken = authService.login(new LoginRequest(ADMIN_EMAIL, PASSWORD)).token();
        editorToken = authService.login(new LoginRequest(EDITOR_EMAIL, PASSWORD)).token();
    }

    @Test
    void uploadWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/v1/media").file(pngFile("foto.png")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void editorCanUploadImage() throws Exception {
        mockMvc.perform(multipart("/api/v1/media")
                        .file(pngFile("foto.png"))
                        .header("Authorization", bearer(editorToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(PNG_BYTES.length))
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("/api/v1/media/files/")));
    }

    @Test
    void uploadedFileIsPubliclyReadable() throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/api/v1/media")
                        .file(pngFile("foto.png"))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isCreated())
                .andReturn();

        String url = JsonPath.read(upload.getResponse().getContentAsString(), "$.url");
        String path = url.substring(url.indexOf("/api/v1/media/files/"));

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void uploadWithUnsupportedTypeReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notas.txt", "text/plain", "contenido".getBytes());

        mockMvc.perform(multipart("/api/v1/media")
                        .file(file)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tipo de archivo no permitido: text/plain"));
    }

    @Test
    void uploadWithEmptyFileReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "vacio.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/v1/media")
                        .file(file)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El archivo está vacío"));
    }

    @Test
    void unknownFileReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/media/files/no-existe.png"))
                .andExpect(status().isNotFound());
    }

    private MockMultipartFile pngFile(String filename) {
        return new MockMultipartFile("file", filename, "image/png", PNG_BYTES);
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
