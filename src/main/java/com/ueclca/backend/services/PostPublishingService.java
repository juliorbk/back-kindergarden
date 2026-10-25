package com.ueclca.backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ueclca.backend.exceptions.ChannelPublishException;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.ChannelDeliveryStatus;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostChannel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostPublishingService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final List<ChannelPublisher> publishers;

    public boolean supports(Channel channel) {
        return findPublisher(channel).isPresent();
    }

    public void publishPending(Post post) {
        if (post.getChannels() == null) {
            return;
        }
        post.getChannels().stream()
                .filter(delivery -> delivery.getStatus() != ChannelDeliveryStatus.SENT)
                .forEach(delivery -> publish(post, delivery));
    }

    public void publish(Post post, PostChannel delivery) {
        if (delivery.getStatus() == ChannelDeliveryStatus.SENT) {
            return;
        }

        Optional<ChannelPublisher> publisher = findPublisher(delivery.getChannel());
        if (publisher.isEmpty()) {
            log.debug("No hay integración disponible para el canal {}; se omite la publicación {}",
                    delivery.getChannel(), post.getId());
            return;
        }

        try {
            String externalId = publisher.get().publish(post);
            delivery.setStatus(ChannelDeliveryStatus.SENT);
            delivery.setExternalId(externalId);
            delivery.setSentAt(Instant.now());
            delivery.setErrorMessage(null);
            log.info("Publicación {} enviada a {} (messageId={})",
                    post.getId(), delivery.getChannel(), externalId);
        } catch (ChannelPublishException ex) {
            delivery.setStatus(ChannelDeliveryStatus.FAILED);
            delivery.setExternalId(null);
            delivery.setSentAt(null);
            delivery.setErrorMessage(truncate(ex.getMessage(), ERROR_MESSAGE_MAX_LENGTH));
            log.warn("No se pudo publicar la publicación {} en {}: {}",
                    post.getId(), delivery.getChannel(), ex.getMessage());
            log.debug("Detalle del fallo al publicar la publicación {} en {}",
                    post.getId(), delivery.getChannel(), ex);
        }
    }

    private Optional<ChannelPublisher> findPublisher(Channel channel) {
        return publishers.stream()
                .filter(publisher -> publisher.channel() == channel)
                .findFirst();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "Error desconocido al publicar en el canal";
        }
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, normalized.offsetByCodePoints(0, maxLength));
    }
}
