package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import com.ueclca.backend.config.TelegramProperties;
import com.ueclca.backend.exceptions.ChannelPublishException;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostMedia;

@ExtendWith(MockitoExtension.class)
class TelegramChannelPublisherTest {

    private static final String COVER_URL = "https://cdn.example.com/foto.jpg";

    private static final byte[] COVER_BYTES = {1, 2, 3, 4};

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private MediaStorage mediaStorage;

    @Test
    void sendsPhotoWhenCoverIsImage() {
        when(telegramClient.sendPhoto(COVER_URL, "Hola")).thenReturn("42");
        TelegramChannelPublisher publisher = publisher(true);

        String externalId = publisher.publish(post("image/png", "Hola"));

        assertEquals("42", externalId);
        verify(telegramClient).sendPhoto(COVER_URL, "Hola");
    }

    @Test
    void sendsVideoWhenCoverIsVideo() {
        when(telegramClient.sendVideo(COVER_URL, "Hola")).thenReturn("43");
        TelegramChannelPublisher publisher = publisher(true);

        String externalId = publisher.publish(post("video/mp4", "Hola"));

        assertEquals("43", externalId);
        verify(telegramClient).sendVideo(COVER_URL, "Hola");
    }

    @Test
    void uploadsStoredCoverAsMultipart() {
        when(mediaStorage.load("foto.png")).thenReturn(new ByteArrayResource(COVER_BYTES));
        when(telegramClient.sendPhoto(COVER_BYTES, "foto.png", "image/png", "Hola")).thenReturn("47");
        TelegramChannelPublisher publisher = publisher(true);

        String externalId = publisher.publish(post("image/png", "Hola", "foto.png"));

        assertEquals("47", externalId);
        verify(telegramClient).sendPhoto(COVER_BYTES, "foto.png", "image/png", "Hola");
        verify(telegramClient, never()).sendPhoto(COVER_URL, "Hola");
    }

    @Test
    void uploadsStoredVideoCoverAsMultipart() {
        when(mediaStorage.load("clip.mp4")).thenReturn(new ByteArrayResource(COVER_BYTES));
        when(telegramClient.sendVideo(COVER_BYTES, "clip.mp4", "video/mp4", "Hola")).thenReturn("48");
        TelegramChannelPublisher publisher = publisher(true);

        String externalId = publisher.publish(post("video/mp4", "Hola", "clip.mp4"));

        assertEquals("48", externalId);
        verify(telegramClient).sendVideo(COVER_BYTES, "clip.mp4", "video/mp4", "Hola");
        verify(telegramClient, never()).sendVideo(COVER_URL, "Hola");
    }

    @Test
    void fallsBackToMessageWhenCaptionExceedsPhotoLimit() {
        String caption = "a".repeat(1025);
        when(telegramClient.sendMessage(anyString())).thenReturn("44");
        TelegramChannelPublisher publisher = publisher(true);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);

        String externalId = publisher.publish(post("image/png", caption));

        assertEquals("44", externalId);
        verify(telegramClient).sendMessage(text.capture());
        assertTrue(text.getValue().contains(COVER_URL));
        verify(telegramClient, never()).sendPhoto(anyString(), anyString());
    }

    @Test
    void usesMessageWhenPostHasNoCover() {
        when(telegramClient.sendMessage("Hola")).thenReturn("45");
        TelegramChannelPublisher publisher = publisher(true);
        Post post = Post.builder().id(1L).caption("Hola").build();

        String externalId = publisher.publish(post);

        assertEquals("45", externalId);
        verify(telegramClient).sendMessage("Hola");
    }

    @Test
    void throwsWhenTelegramIsDisabled() {
        TelegramChannelPublisher publisher = publisher(false);

        assertThrows(ChannelPublishException.class, () -> publisher.publish(post("image/png", "Hola")));
        verifyNoInteractions(telegramClient);
    }

    @Test
    void truncatesLongTextKeepingCoverUrl() {
        String caption = "a".repeat(4096);
        when(telegramClient.sendMessage(anyString())).thenReturn("46");
        TelegramChannelPublisher publisher = publisher(true);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);

        publisher.publish(post("image/png", caption));

        verify(telegramClient).sendMessage(text.capture());
        String message = text.getValue();
        assertTrue(message.endsWith(COVER_URL));
        assertTrue(message.codePointCount(0, message.length()) <= 4096);
    }

    private TelegramChannelPublisher publisher(boolean enabled) {
        TelegramProperties properties = new TelegramProperties(
                enabled, "test-token", "@channel", "https://api.telegram.org", 1000, 1000);
        return new TelegramChannelPublisher(telegramClient, properties, mediaStorage);
    }

    private Post post(String coverMimeType, String caption) {
        return post(coverMimeType, caption, null);
    }

    private Post post(String coverMimeType, String caption, String storageKey) {
        MediaAsset cover = MediaAsset.builder()
                .url(COVER_URL)
                .mimeType(coverMimeType)
                .storageKey(storageKey)
                .build();
        PostMedia media = PostMedia.builder()
                .media(cover)
                .build();
        return Post.builder()
                .id(1L)
                .caption(caption)
                .imageUrl(COVER_URL)
                .media(List.of(media))
                .build();
    }
}
