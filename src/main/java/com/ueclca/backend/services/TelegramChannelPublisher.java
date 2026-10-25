package com.ueclca.backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ueclca.backend.config.TelegramProperties;
import com.ueclca.backend.exceptions.ChannelPublishException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostMedia;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramChannelPublisher implements ChannelPublisher {

    private static final int MEDIA_CAPTION_MAX_LENGTH = 1024;
    private static final int MESSAGE_MAX_LENGTH = 4096;

    private final TelegramClient telegramClient;
    private final TelegramProperties telegramProperties;
    private final MediaStorage mediaStorage;

    @Override
    public Channel channel() {
        return Channel.TELEGRAM;
    }

    @Override
    public String publish(Post post) {
        if (!telegramProperties.enabled()) {
            log.debug("Se omite el envío a Telegram de la publicación {}: integración deshabilitada",
                    post.getId());
            throw new ChannelPublishException("La integración con Telegram no está habilitada");
        }
        if (!telegramProperties.isConfigured()) {
            log.warn("Se intentó publicar en Telegram la publicación {} sin configuración completa",
                    post.getId());
            throw new ChannelPublishException("La integración con Telegram no está configurada");
        }

        String caption = post.getCaption() == null ? "" : post.getCaption().trim();
        String coverUrl = post.getImageUrl();

        if (coverUrl == null || coverUrl.isBlank()) {
            log.debug("Publicación {}: envío a Telegram como mensaje de texto", post.getId());
            return telegramClient.sendMessage(truncate(caption, MESSAGE_MAX_LENGTH));
        }

        MediaAsset cover = findCoverAsset(post);
        boolean stored = cover != null && StringUtils.hasText(cover.getStorageKey());
        if (caption.codePointCount(0, caption.length()) <= MEDIA_CAPTION_MAX_LENGTH) {
            boolean video = cover != null && isVideo(cover);
            log.debug("Publicación {}: envío a Telegram como {} con portada {}", post.getId(),
                    video ? "video" : "foto",
                    stored ? "adjunta" : "remota");
            if (video) {
                return stored
                        ? telegramClient.sendVideo(readCover(cover), cover.getStorageKey(),
                                cover.getMimeType(), caption)
                        : telegramClient.sendVideo(coverUrl, caption);
            }
            return stored
                    ? telegramClient.sendPhoto(readCover(cover), cover.getStorageKey(),
                            cover.getMimeType(), caption)
                    : telegramClient.sendPhoto(coverUrl, caption);
        }

        log.debug("Publicación {}: envío a Telegram como mensaje con enlace a la portada",
                post.getId());
        return telegramClient.sendMessage(messageWithCover(caption, coverUrl));
    }

    private MediaAsset findCoverAsset(Post post) {
        List<PostMedia> media = post.getMedia();
        if (media == null) {
            return null;
        }
        return media.stream()
                .map(PostMedia::getMedia)
                .filter(Objects::nonNull)
                .filter(asset -> post.getImageUrl().equals(asset.getUrl()))
                .findFirst()
                .orElse(null);
    }

    private boolean isVideo(MediaAsset asset) {
        return asset.getMimeType() != null && asset.getMimeType().startsWith("video/");
    }

    private byte[] readCover(MediaAsset asset) {
        try (InputStream input = mediaStorage.load(asset.getStorageKey()).getInputStream()) {
            return input.readAllBytes();
        } catch (IOException | ResourceNotFoundException ex) {
            log.warn("No se pudo leer la portada de la publicación desde {}: {}",
                    asset.getStorageKey(), ex.getMessage());
            throw new ChannelPublishException("No se pudo leer la imagen de la publicación");
        }
    }

    private String messageWithCover(String caption, String coverUrl) {
        String suffix = "\n\n" + coverUrl;
        int maxCaptionLength = MESSAGE_MAX_LENGTH - suffix.length();
        if (maxCaptionLength <= 0) {
            return truncate(coverUrl, MESSAGE_MAX_LENGTH);
        }
        return truncate(caption, maxCaptionLength) + suffix;
    }

    private String truncate(String value, int maxLength) {
        if (value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
