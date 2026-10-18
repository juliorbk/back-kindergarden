package com.ueclca.backend.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.PostRepository;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.AuthService;
import com.ueclca.backend.services.PostService;

@AutoConfigureMockMvc
class PostApiIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ueclca.edu.ve";
    private static final String EDITOR_EMAIL = "editor@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";
    private static final String IMAGE_URL = "https://example.com/imagen.jpg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

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
        postRepository.deleteAll();

        adminToken = authService.login(new LoginRequest(ADMIN_EMAIL, PASSWORD)).token();
        editorToken = authService.login(new LoginRequest(EDITOR_EMAIL, PASSWORD)).token();
    }

    @Test
    void listWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("Feria de Ciencias", IMAGE_URL)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithEditorReturnsCreatedWithGeneratedSlug() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearer(editorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("¡Feria de Ciencias 2026!", IMAGE_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.slug").value("feria-de-ciencias-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.publishedAt").doesNotExist());
    }

    @Test
    void createWithDuplicateTitleAppendsSuffix() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("Título repetido", IMAGE_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("titulo-repetido"));

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody("Título repetido", IMAGE_URL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("titulo-repetido-2"));
    }

    @Test
    void createWithInvalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(" ", "ftp://example.com/imagen.jpg")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.imageUrl").exists());
    }

    @Test
    void updateChangesFieldsButKeepsSlug() throws Exception {
        Post post = createDraft("Feria de Ciencias");

        mockMvc.perform(put("/api/v1/posts/" + post.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Feria de Ciencias 2027","caption":"Nueva descripción","imageUrl":"%s"}
                                """.formatted(IMAGE_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("feria-de-ciencias"))
                .andExpect(jsonPath("$.title").value("Feria de Ciencias 2027"));
    }

    @Test
    void getUnknownIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/posts/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByStatus() throws Exception {
        Post published = createDraft("Publicada");
        createDraft("Borrador");
        postService.changeStatus(published.getId(), PublicationStatus.PUBLISHED);

        mockMvc.perform(get("/api/v1/posts?status=PUBLISHED")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("publicada"));

        mockMvc.perform(get("/api/v1/posts?status=DRAFT")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("borrador"));
    }

    @Test
    void listClampsPageSizeToConfiguredMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/posts?size=100")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void changeStatusWithInvalidValueReturnsBadRequest() throws Exception {
        Post post = createDraft("Publicación");

        mockMvc.perform(patch("/api/v1/posts/" + post.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"NO_EXISTE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicEndpointsOnlyExposePublishedPosts() throws Exception {
        Post published = createDraft("Publicación publicada");
        Post draft = createDraft("Publicación borrador");
        postService.changeStatus(published.getId(), PublicationStatus.PUBLISHED);

        mockMvc.perform(get("/api/v1/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("publicacion-publicada"));

        mockMvc.perform(get("/api/v1/public/posts/" + published.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("publicacion-publicada"));

        mockMvc.perform(get("/api/v1/public/posts/" + draft.getSlug()))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicListOrdersByPublishedAtDescending() throws Exception {
        Post older = createDraft("Más antigua");
        postService.changeStatus(older.getId(), PublicationStatus.PUBLISHED);
        Post newer = createDraft("Más reciente");
        postService.changeStatus(newer.getId(), PublicationStatus.PUBLISHED);

        mockMvc.perform(get("/api/v1/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value("mas-reciente"))
                .andExpect(jsonPath("$.content[1].slug").value("mas-antigua"));
    }

    @Test
    void unpublishHidesPostAndRepublishKeepsOriginalDate() throws Exception {
        Post post = createDraft("Publicación");
        postService.changeStatus(post.getId(), PublicationStatus.PUBLISHED);
        String firstPublishedAt = postService.getById(post.getId()).getPublishedAt().toString();

        mockMvc.perform(patch("/api/v1/posts/" + post.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DRAFT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/public/posts/" + post.getSlug()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/posts/" + post.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PUBLISHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").value(firstPublishedAt));
    }

    @Test
    void deleteRemovesPost() throws Exception {
        Post post = createDraft("Para eliminar");

        mockMvc.perform(delete("/api/v1/posts/" + post.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/posts/" + post.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    private Post createDraft(String title) {
        return postService.create(new PostRequest(title, "Descripción de " + title, IMAGE_URL), ADMIN_EMAIL);
    }

    private String postBody(String title, String imageUrl) {
        return """
                {"title":"%s","caption":"Descripción de prueba","imageUrl":"%s"}
                """.formatted(title, imageUrl);
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
