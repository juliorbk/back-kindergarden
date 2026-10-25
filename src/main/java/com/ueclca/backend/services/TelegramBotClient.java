package com.ueclca.backend.services;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ueclca.backend.config.TelegramProperties;
import com.ueclca.backend.exceptions.ChannelPublishException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@Slf4j
public class TelegramBotClient implements TelegramClient {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final TelegramProperties telegramProperties;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public TelegramBotClient(TelegramProperties telegramProperties, JsonMapper jsonMapper) {
        this.telegramProperties = telegramProperties;
        this.jsonMapper = jsonMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(telegramProperties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(telegramProperties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(telegramProperties.apiBaseUrl())
                .requestFactory(requestFactory)
                .messageConverters((converters) -> {
                    converters.clear();
                    converters.add(new JacksonJsonHttpMessageConverter(jsonMapper));
                    converters.add(new FormHttpMessageConverter());
                })
                .build();
    }

    @Override
    public String sendPhoto(String photoUrl, String caption) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", telegramProperties.chatId());
        payload.put("photo", photoUrl);
        if (caption != null && !caption.isBlank()) {
            payload.put("caption", caption);
        }
        return send("sendPhoto", payload);
    }

    @Override
    public String sendPhoto(byte[] content, String filename, String contentType, String caption) {
        return sendFile("sendPhoto", "photo", content, filename, contentType, caption);
    }

    @Override
    public String sendVideo(String videoUrl, String caption) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", telegramProperties.chatId());
        payload.put("video", videoUrl);
        if (caption != null && !caption.isBlank()) {
            payload.put("caption", caption);
        }
        return send("sendVideo", payload);
    }

    @Override
    public String sendVideo(byte[] content, String filename, String contentType, String caption) {
        return sendFile("sendVideo", "video", content, filename, contentType, caption);
    }

    @Override
    public String sendMessage(String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", telegramProperties.chatId());
        payload.put("text", text);
        return send("sendMessage", payload);
    }

    private String sendFile(String method, String partName, byte[] content, String filename,
            String contentType, String caption) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("chat_id", telegramProperties.chatId());
        if (StringUtils.hasText(caption)) {
            parts.add("caption", caption);
        }

        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(contentType));
        parts.add(partName, new HttpEntity<>(resource, headers));
        return sendMultipart(method, parts);
    }

    private String sendMultipart(String method, MultiValueMap<String, Object> payload) {
        log.debug("Invocando la API de Telegram: método={} (multipart)", method);
        try {
            return restClient.post()
                    .uri("/bot{token}/{method}", telegramProperties.botToken(), method)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, response) -> parseResponse(method, response.getBody().readAllBytes()));
        } catch (RestClientException ex) {
            log.warn("No se pudo conectar con Telegram al invocar {}: {}", method,
                    ex.getClass().getSimpleName());
            throw new ChannelPublishException("No se pudo conectar con Telegram");
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String send(String method, Map<String, Object> payload) {
        log.debug("Invocando la API de Telegram: método={}", method);
        try {
            return restClient.post()
                    .uri("/bot{token}/{method}", telegramProperties.botToken(), method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, response) -> parseResponse(method, response.getBody().readAllBytes()));
        } catch (RestClientException ex) {
            log.warn("No se pudo conectar con Telegram al invocar {}: {}", method,
                    ex.getClass().getSimpleName());
            throw new ChannelPublishException("No se pudo conectar con Telegram");
        }
    }

    private String parseResponse(String method, byte[] responseBody) {
        if (responseBody.length == 0) {
            log.warn("Telegram devolvió una respuesta vacía al invocar {}", method);
            throw new ChannelPublishException("Telegram devolvió una respuesta vacía");
        }

        JsonNode root;
        try {
            root = jsonMapper.readTree(responseBody);
        } catch (JacksonException ex) {
            log.warn("Telegram devolvió una respuesta inválida al invocar {} ({} bytes)",
                    method, responseBody.length);
            throw new ChannelPublishException("Telegram devolvió una respuesta inválida");
        }

        if (root == null || !root.path("ok").asBoolean(false)) {
            String description = root == null ? "" : root.path("description").asText("");
            log.warn("Telegram rechazó la solicitud {}: {}", method,
                    description.isBlank() ? "sin descripción" : description);
            throw new ChannelPublishException(description.isBlank()
                    ? "Telegram rechazó la publicación"
                    : truncate("Telegram: " + description, ERROR_MESSAGE_MAX_LENGTH));
        }

        long messageId = root.path("result").path("message_id").asLong(0);
        if (messageId <= 0) {
            log.warn("Telegram no devolvió el identificador del mensaje al invocar {}", method);
            throw new ChannelPublishException("Telegram no devolvió el identificador del mensaje");
        }
        log.debug("Telegram aceptó la solicitud {} (messageId={})", method, messageId);
        return Long.toString(messageId);
    }

    private String truncate(String value, int maxLength) {
        if (value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
