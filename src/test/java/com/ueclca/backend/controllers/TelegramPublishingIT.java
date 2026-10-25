package com.ueclca.backend.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.sun.net.httpserver.HttpServer;
import com.ueclca.backend.AbstractIntegrationTest;
import com.ueclca.backend.dtos.LoginRequest;
import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.MediaAssetRepository;
import com.ueclca.backend.repositories.PostRepository;
import com.ueclca.backend.repositories.UserRepository;
import com.ueclca.backend.services.AuthService;
import com.ueclca.backend.services.MediaService;
import com.ueclca.backend.services.PostService;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TelegramPublishingIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ueclca.edu.ve";
    private static final String PASSWORD = "admin1234";
    private static final String BOT_TOKEN = "test-token";
    private static final String CHAT_ID = "@test-channel";
    private static final String SUCCESS_BODY = "{\"ok\":true,\"result\":{\"message_id\":42}}";
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private static final AtomicReference<String> RESPONSE_BODY = new AtomicReference<>(SUCCESS_BODY);
    private static final AtomicInteger RESPONSE_STATUS = new AtomicInteger(200);
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger();
    private static final AtomicReference<String> LAST_PATH = new AtomicReference<>();
    private static final AtomicReference<String> LAST_BODY = new AtomicReference<>();
    private static final AtomicReference<String> LAST_CONTENT_TYPE = new AtomicReference<>();
    private static final HttpServer TELEGRAM_STUB = startStub();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private PostService postService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @DynamicPropertySource
    static void telegramProperties(DynamicPropertyRegistry registry) {
        registry.add("app.telegram.enabled", () -> true);
        registry.add("app.telegram.bot-token", () -> BOT_TOKEN);
        registry.add("app.telegram.chat-id", () -> CHAT_ID);
        registry.add("app.telegram.api-base-url",
                () -> "http://127.0.0.1:" + TELEGRAM_STUB.getAddress().getPort());
        registry.add("app.telegram.connect-timeout-ms", () -> 2000);
        registry.add("app.telegram.read-timeout-ms", () -> 5000);
    }

    @AfterAll
    static void stopStub() {
        TELEGRAM_STUB.stop(0);
    }

    @BeforeEach
    void setUp() {
        RESPONSE_BODY.set(SUCCESS_BODY);
        RESPONSE_STATUS.set(200);
        REQUEST_COUNT.set(0);
        LAST_PATH.set(null);
        LAST_BODY.set(null);
        LAST_CONTENT_TYPE.set(null);

        seedAdmin();
        postRepository.deleteAll();
        mediaAssetRepository.deleteAll();
        adminToken = authService.login(new LoginRequest(ADMIN_EMAIL, PASSWORD)).token();
    }

    @Test
    void publishesPostToTelegramWhenPublished() throws Exception {
        Post post = createDraft("Hola desde la escuela", Set.of(Channel.TELEGRAM));

        mockMvc.perform(patch("/api/v1/posts/" + post.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PUBLISHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.channels", hasSize(1)))
                .andExpect(jsonPath("$.channels[0].channel").value("TELEGRAM"))
                .andExpect(jsonPath("$.channels[0].status").value("SENT"))
                .andExpect(jsonPath("$.channels[0].externalId").value("42"))
                .andExpect(jsonPath("$.channels[0].sentAt").exists());

        assertEquals(1, REQUEST_COUNT.get());
        assertEquals("/bot" + BOT_TOKEN + "/sendPhoto", LAST_PATH.get());
        assertNotNull(LAST_BODY.get());
        assertTrue(LAST_CONTENT_TYPE.get().startsWith("multipart/form-data"));
        assertTrue(LAST_BODY.get().contains("name=\"chat_id\""));
        assertTrue(LAST_BODY.get().contains("name=\"caption\""));
        assertTrue(LAST_BODY.get().contains("Hola desde la escuela"));
        assertTrue(LAST_BODY.get().contains("name=\"photo\"; filename=\""));
        assertFalse(LAST_BODY.get().contains("/api/v1/media/files/"));
        assertFalse(LAST_BODY.get().contains("localhost"));
    }

    @Test
    void doesNotResendChannelAlreadySent() throws Exception {
        Post post = createDraft("Hola", Set.of(Channel.TELEGRAM));
        publish(post);
        assertEquals(1, REQUEST_COUNT.get());

        publish(post);

        assertEquals(1, REQUEST_COUNT.get());
        mockMvc.perform(get("/api/v1/posts/" + post.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0].status").value("SENT"));
    }

    @Test
    void marksChannelFailedButKeepsPostPublishedWhenTelegramFails() throws Exception {
        RESPONSE_STATUS.set(400);
        RESPONSE_BODY.set("""
                {"ok":false,"error_code":400,"description":"Bad Request: chat not found"}
                """);
        Post post = createDraft("Hola", Set.of(Channel.TELEGRAM));

        mockMvc.perform(patch("/api/v1/posts/" + post.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PUBLISHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.channels[0].status").value("FAILED"))
                .andExpect(jsonPath("$.channels[0].errorMessage", containsString("chat not found")))
                .andExpect(jsonPath("$.channels[0].sentAt").doesNotExist());

        assertEquals(1, REQUEST_COUNT.get());
    }

    @Test
    void retrySendsFailedChannelAgain() throws Exception {
        RESPONSE_STATUS.set(400);
        RESPONSE_BODY.set("""
                {"ok":false,"description":"Bad Request: chat not found"}
                """);
        Post post = createDraft("Hola", Set.of(Channel.TELEGRAM));
        publish(post);
        assertEquals(1, REQUEST_COUNT.get());

        RESPONSE_STATUS.set(200);
        RESPONSE_BODY.set(SUCCESS_BODY);

        mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/channels/TELEGRAM/retry")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[0].status").value("SENT"))
                .andExpect(jsonPath("$.channels[0].externalId").value("42"));

        assertEquals(2, REQUEST_COUNT.get());
    }

    @Test
    void retryRejectsAlreadySentChannel() throws Exception {
        Post post = createDraft("Hola", Set.of(Channel.TELEGRAM));
        publish(post);

        mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/channels/TELEGRAM/retry")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void retryRejectsDraftPost() throws Exception {
        Post post = createDraft("Hola", Set.of(Channel.TELEGRAM));

        mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/channels/TELEGRAM/retry")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void retryRejectsUnsupportedChannel() throws Exception {
        Post post = createDraft("Hola", Set.of(Channel.FACEBOOK));
        postService.changeStatus(post.getId(), PublicationStatus.PUBLISHED);

        mockMvc.perform(post("/api/v1/posts/" + post.getId() + "/channels/FACEBOOK/retry")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    private Post createDraft(String caption, Set<Channel> channels) {
        MockMultipartFile file = new MockMultipartFile("file", "foto.png", "image/png", PNG_BYTES);
        MediaAsset media = mediaService.upload(file, ADMIN_EMAIL);
        return postService.create(new PostRequest(
                null, caption, null, List.of(media.getId()), channels), ADMIN_EMAIL);
    }

    private void publish(Post post) throws Exception {
        mockMvc.perform(patch("/api/v1/posts/" + post.getId() + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PUBLISHED"}
                                """))
                .andExpect(status().isOk());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void seedAdmin() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            userRepository.save(User.builder()
                    .email(ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .fullName("Administrador de prueba")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                REQUEST_COUNT.incrementAndGet();
                LAST_PATH.set(exchange.getRequestURI().getPath());
                LAST_CONTENT_TYPE.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                LAST_BODY.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

                byte[] body = RESPONSE_BODY.get().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(RESPONSE_STATUS.get(), body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo iniciar el stub de Telegram", ex);
        }
    }
}
