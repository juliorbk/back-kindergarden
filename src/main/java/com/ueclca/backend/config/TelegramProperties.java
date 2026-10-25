package com.ueclca.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(
        boolean enabled,
        String botToken,
        String chatId,
        String apiBaseUrl,
        @Positive(message = "TELEGRAM_CONNECT_TIMEOUT_MS debe ser mayor que cero")
        int connectTimeoutMs,
        @Positive(message = "TELEGRAM_READ_TIMEOUT_MS debe ser mayor que cero")
        int readTimeoutMs) {

    public TelegramProperties {
        apiBaseUrl = StringUtils.hasText(apiBaseUrl)
                ? apiBaseUrl.replaceAll("/+$", "")
                : "https://api.telegram.org";
        botToken = botToken == null ? "" : botToken;
        chatId = chatId == null ? "" : chatId;
    }

    @AssertTrue(message = "TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID son obligatorios cuando Telegram está habilitado")
    public boolean isConfigurationValid() {
        return !enabled || isConfigured();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(botToken) && StringUtils.hasText(chatId);
    }
}
