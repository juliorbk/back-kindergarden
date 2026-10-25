package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ueclca.backend.exceptions.ChannelPublishException;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.ChannelDeliveryStatus;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostChannel;

@ExtendWith(MockitoExtension.class)
class PostPublishingServiceTest {

    @Mock
    private ChannelPublisher publisher;

    @Test
    void sentChannelIsSkipped() {
        Post post = post();
        PostChannel delivery = delivery(post, Channel.TELEGRAM, ChannelDeliveryStatus.SENT);
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        service.publishPending(post);

        verifyNoInteractions(publisher);
    }

    @Test
    void successfulPublishMarksChannelAsSent() {
        Post post = post();
        PostChannel delivery = delivery(post, Channel.TELEGRAM, ChannelDeliveryStatus.PENDING);
        when(publisher.channel()).thenReturn(Channel.TELEGRAM);
        when(publisher.publish(post)).thenReturn("42");
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        service.publishPending(post);

        assertEquals(ChannelDeliveryStatus.SENT, delivery.getStatus());
        assertEquals("42", delivery.getExternalId());
        assertNotNull(delivery.getSentAt());
        assertNull(delivery.getErrorMessage());
    }

    @Test
    void failedPublishMarksChannelAsFailedWithoutThrowing() {
        Post post = post();
        PostChannel delivery = delivery(post, Channel.TELEGRAM, ChannelDeliveryStatus.PENDING);
        when(publisher.channel()).thenReturn(Channel.TELEGRAM);
        when(publisher.publish(post)).thenThrow(new ChannelPublishException("Telegram: chat not found"));
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        service.publishPending(post);

        assertEquals(ChannelDeliveryStatus.FAILED, delivery.getStatus());
        assertNull(delivery.getExternalId());
        assertNull(delivery.getSentAt());
        assertEquals("Telegram: chat not found", delivery.getErrorMessage());
    }

    @Test
    void failedChannelIsRetried() {
        Post post = post();
        PostChannel delivery = delivery(post, Channel.TELEGRAM, ChannelDeliveryStatus.FAILED);
        when(publisher.channel()).thenReturn(Channel.TELEGRAM);
        when(publisher.publish(post)).thenReturn("99");
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        service.publishPending(post);

        assertEquals(ChannelDeliveryStatus.SENT, delivery.getStatus());
        assertEquals("99", delivery.getExternalId());
    }

    @Test
    void channelWithoutPublisherRemainsPending() {
        Post post = post();
        PostChannel delivery = delivery(post, Channel.FACEBOOK, ChannelDeliveryStatus.PENDING);
        when(publisher.channel()).thenReturn(Channel.TELEGRAM);
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        service.publishPending(post);

        assertEquals(ChannelDeliveryStatus.PENDING, delivery.getStatus());
        verify(publisher, never()).publish(any(Post.class));
    }

    @Test
    void errorMessageIsTruncated() {
        Post post = post();
        PostChannel delivery = delivery(post, Channel.TELEGRAM, ChannelDeliveryStatus.PENDING);
        when(publisher.channel()).thenReturn(Channel.TELEGRAM);
        when(publisher.publish(post)).thenThrow(new ChannelPublishException("x".repeat(600)));
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        service.publishPending(post);

        assertEquals(ChannelDeliveryStatus.FAILED, delivery.getStatus());
        assertNotNull(delivery.getErrorMessage());
        assertTrue(delivery.getErrorMessage().length() <= 500);
    }

    @Test
    void supportsReturnsTrueOnlyForRegisteredPublishers() {
        when(publisher.channel()).thenReturn(Channel.TELEGRAM);
        PostPublishingService service = new PostPublishingService(List.of(publisher));

        assertTrue(service.supports(Channel.TELEGRAM));
        assertFalse(service.supports(Channel.FACEBOOK));
    }

    private Post post() {
        return Post.builder()
                .id(1L)
                .caption("Descripción")
                .imageUrl("https://example.com/imagen.jpg")
                .build();
    }

    private PostChannel delivery(Post post, Channel channel, ChannelDeliveryStatus status) {
        PostChannel delivery = PostChannel.builder()
                .post(post)
                .channel(channel)
                .status(status)
                .build();
        if (post.getChannels() == null) {
            post.setChannels(new ArrayList<>());
        }
        post.getChannels().add(delivery);
        return delivery;
    }
}
