package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import com.ueclca.backend.config.TelegramProperties;
import com.ueclca.backend.exceptions.ChannelPublishException;

import tools.jackson.databind.json.JsonMapper;

class TelegramBotClientTest {

    private static final String BOT_TOKEN = "test-token";
    private static final String CHAT_ID = "@test-channel";

    private final AtomicReference<String> responseBody = new AtomicReference<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();

    private HttpServer server;
    private TelegramBotClient client;

    @BeforeEach
    void setUp() throws IOException {
        responseBody.set("{\"ok\":true,\"result\":{\"message_id\":42}}");
        responseStatus.set(200);
        lastPath.set(null);
        lastBody.set(null);
        lastContentType.set(null);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        TelegramProperties properties = new TelegramProperties(
                true, BOT_TOKEN, CHAT_ID,
                "http://127.0.0.1:" + server.getAddress().getPort(), 2000, 2000);
        client = new TelegramBotClient(properties, JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendPhotoPostsUtf8JsonAndReturnsMessageId() {
        String caption = "¡Hola! ñ";

        String messageId = client.sendPhoto("https://cdn.example.com/foto.jpg", caption);

        assertEquals("42", messageId);
        assertEquals("/bot" + BOT_TOKEN + "/sendPhoto", lastPath.get());
        assertTrue(lastBody.get().contains("\"chat_id\":\"" + CHAT_ID + "\""));
        assertTrue(lastBody.get().contains("\"photo\":\"https://cdn.example.com/foto.jpg\""));
        assertTrue(lastBody.get().contains("\"caption\":\"" + caption + "\""));
    }

    @Test
    void sendPhotoUploadsStoredFileAsMultipart() {
        byte[] content = "fake-image".getBytes(StandardCharsets.UTF_8);

        String messageId = client.sendPhoto(content, "foto.png", "image/png", "Hola");

        assertEquals("42", messageId);
        assertEquals("/bot" + BOT_TOKEN + "/sendPhoto", lastPath.get());
        assertTrue(lastContentType.get().startsWith("multipart/form-data"));
        String body = lastBody.get();
        assertTrue(body.contains("name=\"chat_id\""));
        assertTrue(body.contains("name=\"caption\""));
        assertTrue(body.contains("name=\"photo\"; filename=\"foto.png\""));
        assertTrue(body.contains("fake-image"));
    }

    @Test
    void sendVideoUsesVideoEndpoint() {
        client.sendVideo("https://cdn.example.com/video.mp4", "Hola");

        assertEquals("/bot" + BOT_TOKEN + "/sendVideo", lastPath.get());
        assertTrue(lastBody.get().contains("\"video\":\"https://cdn.example.com/video.mp4\""));
    }

    @Test
    void sendVideoUploadsStoredFileAsMultipart() {
        byte[] content = "fake-video".getBytes(StandardCharsets.UTF_8);

        String messageId = client.sendVideo(content, "clip.mp4", "video/mp4", "Hola");

        assertEquals("42", messageId);
        assertEquals("/bot" + BOT_TOKEN + "/sendVideo", lastPath.get());
        assertTrue(lastContentType.get().startsWith("multipart/form-data"));
        String body = lastBody.get();
        assertTrue(body.contains("name=\"video\"; filename=\"clip.mp4\""));
        assertTrue(body.contains("fake-video"));
    }

    @Test
    void sendMessageUsesTextEndpoint() {
        client.sendMessage("Hola");

        assertEquals("/bot" + BOT_TOKEN + "/sendMessage", lastPath.get());
        assertTrue(lastBody.get().contains("\"text\":\"Hola\""));
    }

    @Test
    void apiErrorReturnsSanitizedMessage() {
        responseStatus.set(400);
        responseBody.set("""
                {"ok":false,"error_code":400,"description":"Bad Request: chat not found"}
                """);

        ChannelPublishException ex = assertThrows(ChannelPublishException.class,
                () -> client.sendMessage("Hola"));

        assertTrue(ex.getMessage().contains("chat not found"));
        assertFalse(ex.getMessage().contains(BOT_TOKEN));
    }

    @Test
    void invalidResponseReturnsSanitizedMessage() {
        responseBody.set("no-es-json");

        ChannelPublishException ex = assertThrows(ChannelPublishException.class,
                () -> client.sendMessage("Hola"));

        assertEquals("Telegram devolvió una respuesta inválida", ex.getMessage());
        assertFalse(ex.getMessage().contains(BOT_TOKEN));
    }

    @Test
    void emptyResponseReturnsSanitizedMessage() {
        responseBody.set("");

        ChannelPublishException ex = assertThrows(ChannelPublishException.class,
                () -> client.sendMessage("Hola"));

        assertEquals("Telegram devolvió una respuesta vacía", ex.getMessage());
    }

    @Test
    void missingMessageIdReturnsSanitizedMessage() {
        responseBody.set("{\"ok\":true,\"result\":{}}");

        ChannelPublishException ex = assertThrows(ChannelPublishException.class,
                () -> client.sendMessage("Hola"));

        assertEquals("Telegram no devolvió el identificador del mensaje", ex.getMessage());
    }

    @Test
    void connectionErrorReturnsSanitizedMessage() {
        TelegramProperties properties = new TelegramProperties(
                true, BOT_TOKEN, CHAT_ID, "http://127.0.0.1:1", 300, 300);
        TelegramBotClient failingClient = new TelegramBotClient(properties, JsonMapper.builder().build());

        ChannelPublishException ex = assertThrows(ChannelPublishException.class,
                () -> failingClient.sendMessage("Hola"));

        assertEquals("No se pudo conectar con Telegram", ex.getMessage());
        assertFalse(ex.getMessage().contains(BOT_TOKEN));
    }
}
