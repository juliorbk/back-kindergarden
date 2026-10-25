package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ConflictException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.ChannelDeliveryStatus;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostChannel;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.Role;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.MediaAssetRepository;
import com.ueclca.backend.repositories.PostRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private UserService userService;

    @Mock
    private PostPublishingService postPublishingService;

    @InjectMocks
    private PostService postService;

    @Test
    void createUsesUniqueSlugAndStartsAsDraft() {
        User author = User.builder()
                .email("editor@ueclca.edu.ve")
                .fullName("Editor")
                .role(Role.EDITOR)
                .enabled(true)
                .build();
        when(userService.getByEmail("editor@ueclca.edu.ve")).thenReturn(author);
        when(postRepository.existsBySlug("feria-de-ciencias")).thenReturn(true);
        when(postRepository.existsBySlug("feria-de-ciencias-2")).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post post = postService.create(new PostRequest(
                "Feria de Ciencias", "Descripción", "https://example.com/feria.jpg",
                List.of(), Set.of(Channel.TELEGRAM)), "editor@ueclca.edu.ve");

        assertEquals("feria-de-ciencias-2", post.getSlug());
        assertEquals(PublicationStatus.DRAFT, post.getStatus());
        assertNotNull(post.getAuthor());
    }

    @Test
    void createUsesFirstImageAsCoverWhenGalleryStartsWithVideo() {
        User author = User.builder()
                .email("editor@ueclca.edu.ve")
                .fullName("Editor")
                .role(Role.EDITOR)
                .enabled(true)
                .build();
        MediaAsset video = MediaAsset.builder()
                .id(1L)
                .storageKey("video.mp4")
                .url("http://localhost:8080/api/v1/media/files/video.mp4")
                .mimeType("video/mp4")
                .sizeBytes(100)
                .build();
        MediaAsset image = MediaAsset.builder()
                .id(2L)
                .storageKey("foto.jpg")
                .url("http://localhost:8080/api/v1/media/files/foto.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(100)
                .build();
        when(userService.getByEmail("editor@ueclca.edu.ve")).thenReturn(author);
        when(postRepository.existsBySlug(any(String.class))).thenReturn(false);
        when(mediaAssetRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(video, image));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post post = postService.create(new PostRequest(
                "Galería", "Descripción", null, List.of(1L, 2L), Set.of(Channel.TELEGRAM)),
                "editor@ueclca.edu.ve");

        assertEquals(image.getUrl(), post.getImageUrl());
    }

    @Test
    void changeStatusSetsPublishedAtOnlyOnce() {
        Post post = Post.builder()
                .title("Publicación")
                .status(PublicationStatus.DRAFT)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.changeStatus(1L, PublicationStatus.PUBLISHED);
        Instant firstPublishedAt = post.getPublishedAt();
        assertNotNull(firstPublishedAt);

        postService.changeStatus(1L, PublicationStatus.DRAFT);
        postService.changeStatus(1L, PublicationStatus.PUBLISHED);

        assertEquals(firstPublishedAt, post.getPublishedAt());
    }

    @Test
    void getPublishedBySlugRejectsDrafts() {
        Post draft = Post.builder()
                .slug("borrador")
                .status(PublicationStatus.DRAFT)
                .build();
        when(postRepository.findBySlug("borrador")).thenReturn(Optional.of(draft));

        assertThrows(ResourceNotFoundException.class, () -> postService.getPublishedBySlug("borrador"));
    }

    @Test
    void changeStatusToPublishedTriggersChannelPublishing() {
        Post post = postWithChannel(PublicationStatus.DRAFT, Channel.TELEGRAM, ChannelDeliveryStatus.PENDING);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.changeStatus(1L, PublicationStatus.PUBLISHED);

        verify(postPublishingService).publishPending(post);
    }

    @Test
    void changeStatusToDraftDoesNotPublishChannels() {
        Post post = postWithChannel(PublicationStatus.PUBLISHED, Channel.TELEGRAM, ChannelDeliveryStatus.PENDING);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.changeStatus(1L, PublicationStatus.DRAFT);

        verify(postPublishingService, never()).publishPending(any(Post.class));
    }

    @Test
    void retryChannelRejectsDraftPost() {
        Post post = postWithChannel(PublicationStatus.DRAFT, Channel.TELEGRAM, ChannelDeliveryStatus.FAILED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(ConflictException.class, () -> postService.retryChannel(1L, Channel.TELEGRAM));
    }

    @Test
    void retryChannelRejectsAlreadySentChannel() {
        Post post = postWithChannel(PublicationStatus.PUBLISHED, Channel.TELEGRAM, ChannelDeliveryStatus.SENT);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(ConflictException.class, () -> postService.retryChannel(1L, Channel.TELEGRAM));
    }

    @Test
    void retryChannelRejectsUnsupportedChannel() {
        Post post = postWithChannel(PublicationStatus.PUBLISHED, Channel.FACEBOOK, ChannelDeliveryStatus.FAILED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postPublishingService.supports(Channel.FACEBOOK)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> postService.retryChannel(1L, Channel.FACEBOOK));
    }

    @Test
    void retryChannelRejectsUnknownChannel() {
        Post post = postWithChannel(PublicationStatus.PUBLISHED, Channel.TELEGRAM, ChannelDeliveryStatus.FAILED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(ResourceNotFoundException.class, () -> postService.retryChannel(1L, Channel.FACEBOOK));
    }

    @Test
    void retryChannelPublishesFailedDelivery() {
        Post post = postWithChannel(PublicationStatus.PUBLISHED, Channel.TELEGRAM, ChannelDeliveryStatus.FAILED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postPublishingService.supports(Channel.TELEGRAM)).thenReturn(true);

        postService.retryChannel(1L, Channel.TELEGRAM);

        verify(postPublishingService).publish(post, post.getChannels().get(0));
    }

    @Test
    void searchPublishedSortsByPublishedAtDescending() {
        when(postRepository.findByStatus(eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(Page.empty());

        postService.searchPublished(PageRequest.of(0, 20, Sort.by("title")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByStatus(eq(PublicationStatus.PUBLISHED), captor.capture());

        Sort.Order order = captor.getValue().getSort().getOrderFor("publishedAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    private Post postWithChannel(PublicationStatus status, Channel channel, ChannelDeliveryStatus deliveryStatus) {
        Post post = Post.builder()
                .id(1L)
                .slug("publicacion")
                .title("Publicación")
                .caption("Descripción")
                .imageUrl("https://example.com/imagen.jpg")
                .status(status)
                .build();
        PostChannel delivery = PostChannel.builder()
                .post(post)
                .channel(channel)
                .status(deliveryStatus)
                .build();
        post.setChannels(new ArrayList<>(List.of(delivery)));
        return post;
    }
}
