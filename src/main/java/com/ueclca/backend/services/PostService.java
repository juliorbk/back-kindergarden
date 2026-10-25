package com.ueclca.backend.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ConflictException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.ChannelDeliveryStatus;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PostChannel;
import com.ueclca.backend.models.PostMedia;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.MediaAssetRepository;
import com.ueclca.backend.repositories.PostRepository;
import com.ueclca.backend.utils.SlugUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private static final int COVER_URL_MAX_LENGTH = 500;
    private static final int INSTAGRAM_CAPTION_MAX_LENGTH = 2200;
    private static final int DERIVED_TITLE_MAX_LENGTH = 60;
    private static final int MAX_MEDIA_ITEMS = 10;

    private final PostRepository postRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final UserService userService;
    private final PostPublishingService postPublishingService;

    public Page<Post> search(PublicationStatus status, Pageable pageable) {
        Page<Post> posts = status == null
                ? postRepository.findAll(pageable)
                : postRepository.findByStatus(status, pageable);
        posts.forEach(this::initializeDetails);
        return posts;
    }

    public Post getById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));
        initializeDetails(post);
        return post;
    }

    @Transactional
    public Post create(PostRequest request, String authorEmail) {
        User author = userService.getByEmail(authorEmail);
        String caption = request.caption().trim();
        String title = resolveTitle(request.title(), caption);
        validateChannelLimits(request.channels(), caption);
        List<MediaAsset> media = loadMedia(request.mediaIds());
        String coverUrl = resolveCoverUrl(media, request.imageUrl());

        Post post = Post.builder()
                .slug(SlugUtils.uniqueSlug(title, postRepository::existsBySlug))
                .title(title)
                .caption(caption)
                .imageUrl(coverUrl)
                .status(PublicationStatus.DRAFT)
                .author(author)
                .build();

        syncChannels(post, request.channels());
        syncMedia(post, media);
        Post saved = postRepository.save(post);
        log.info("Publicación {} creada por {} (canales={})", saved.getId(), authorEmail,
                request.channels());
        return saved;
    }

    @Transactional
    public Post update(Long id, PostRequest request) {
        Post post = getById(id);
        String caption = request.caption().trim();
        validateChannelLimits(request.channels(), caption);
        List<MediaAsset> media = loadMedia(request.mediaIds());

        post.setTitle(resolveTitle(request.title(), caption));
        post.setCaption(caption);
        post.setImageUrl(resolveCoverUrl(media, request.imageUrl()));
        syncChannels(post, request.channels());
        syncMedia(post, media);
        log.info("Publicación {} actualizada (canales={})", id, request.channels());
        return post;
    }

    @Transactional
    public Post changeStatus(Long id, PublicationStatus status) {
        Post post = getById(id);
        PublicationStatus previous = post.getStatus();
        if (previous != status) {
            if (status == PublicationStatus.PUBLISHED && post.getPublishedAt() == null) {
                post.setPublishedAt(Instant.now());
            }
            post.setStatus(status);
            log.info("Publicación {} cambió de {} a {}", id, previous, status);
        }
        if (status == PublicationStatus.PUBLISHED) {
            postPublishingService.publishPending(post);
        }
        return post;
    }

    @Transactional
    public Post retryChannel(Long id, Channel channel) {
        Post post = getById(id);
        PostChannel delivery = post.getChannels().stream()
                .filter(item -> item.getChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Canal no asociado a la publicación"));

        if (post.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ConflictException("La publicación debe estar publicada para reintentar el envío");
        }
        if (delivery.getStatus() == ChannelDeliveryStatus.SENT) {
            throw new ConflictException("La publicación ya fue enviada a este canal");
        }
        if (!postPublishingService.supports(channel)) {
            throw new BadRequestException(
                    "La integración con " + channel + " aún no está disponible");
        }

        log.info("Reintentando el envío de la publicación {} al canal {} (estado previo={})",
                id, channel, delivery.getStatus());
        postPublishingService.publish(post, delivery);
        return post;
    }

    @Transactional
    public void delete(Long id) {
        postRepository.delete(getById(id));
        log.info("Publicación {} eliminada", id);
    }

    public Page<Post> searchPublished(Pageable pageable) {
        Pageable sortedByPublishedAt = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<Post> posts = postRepository.findByStatus(PublicationStatus.PUBLISHED, sortedByPublishedAt);
        posts.forEach(this::initializeDetails);
        return posts;
    }

    public Post getPublishedBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));
        if (post.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Publicación no encontrada");
        }
        initializeDetails(post);
        return post;
    }

    private void initializeDetails(Post post) {
        post.getChannels().size();
        post.getMedia().forEach(item -> item.getMedia().getUrl());
    }

    private String resolveTitle(String rawTitle, String caption) {
        if (StringUtils.hasText(rawTitle)) {
            return rawTitle.trim();
        }
        String normalized = caption.replaceAll("\\s+", " ");
        if (normalized.length() <= DERIVED_TITLE_MAX_LENGTH) {
            return normalized;
        }
        int cut = normalized.lastIndexOf(' ', DERIVED_TITLE_MAX_LENGTH);
        String derived = cut > 0
                ? normalized.substring(0, cut)
                : normalized.substring(0, DERIVED_TITLE_MAX_LENGTH);
        return derived.trim();
    }

    private void validateChannelLimits(Set<Channel> channels, String caption) {
        if (channels.contains(Channel.INSTAGRAM) && caption.length() > INSTAGRAM_CAPTION_MAX_LENGTH) {
            throw new BadRequestException(
                    "Instagram permite un máximo de " + INSTAGRAM_CAPTION_MAX_LENGTH + " caracteres");
        }
    }

    private List<MediaAsset> loadMedia(List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }
        if (mediaIds.size() > MAX_MEDIA_ITEMS) {
            throw new BadRequestException("La publicación no puede tener más de " + MAX_MEDIA_ITEMS + " medios");
        }
        if (new HashSet<>(mediaIds).size() != mediaIds.size()) {
            throw new BadRequestException("No se puede repetir el mismo medio en la publicación");
        }

        Map<Long, MediaAsset> found = mediaAssetRepository.findAllById(mediaIds).stream()
                .collect(Collectors.toMap(MediaAsset::getId, Function.identity()));

        List<MediaAsset> ordered = new ArrayList<>(mediaIds.size());
        for (Long mediaId : mediaIds) {
            MediaAsset asset = found.get(mediaId);
            if (asset == null) {
                throw new ResourceNotFoundException("Medio no encontrado: " + mediaId);
            }
            ordered.add(asset);
        }
        return ordered;
    }

    private String resolveCoverUrl(List<MediaAsset> media, String imageUrl) {
        String coverUrl;
        if (!media.isEmpty()) {
            coverUrl = media.stream()
                    .filter(asset -> asset.getMimeType().startsWith("image/"))
                    .findFirst()
                    .orElse(media.get(0))
                    .getUrl();
        } else if (StringUtils.hasText(imageUrl)) {
            coverUrl = imageUrl.trim();
        } else {
            throw new BadRequestException("La publicación debe incluir al menos una imagen");
        }
        if (coverUrl.length() > COVER_URL_MAX_LENGTH) {
            throw new BadRequestException(
                    "La URL de la imagen no puede superar los " + COVER_URL_MAX_LENGTH + " caracteres");
        }
        return coverUrl;
    }

    private void syncChannels(Post post, Set<Channel> requested) {
        post.getChannels().removeIf(item -> !requested.contains(item.getChannel()));
        Set<Channel> current = post.getChannels().stream()
                .map(PostChannel::getChannel)
                .collect(Collectors.toSet());
        requested.stream()
                .filter(channel -> !current.contains(channel))
                .forEach(channel -> post.getChannels().add(PostChannel.builder()
                        .post(post)
                        .channel(channel)
                        .status(ChannelDeliveryStatus.PENDING)
                        .build()));
    }

    private void syncMedia(Post post, List<MediaAsset> media) {
        Set<Long> requestedIds = media.stream().map(MediaAsset::getId).collect(Collectors.toSet());
        post.getMedia().removeIf(item -> !requestedIds.contains(item.getMedia().getId()));

        Map<Long, PostMedia> current = post.getMedia().stream()
                .collect(Collectors.toMap(item -> item.getMedia().getId(), Function.identity()));
        for (int index = 0; index < media.size(); index++) {
            MediaAsset asset = media.get(index);
            PostMedia item = current.get(asset.getId());
            if (item == null) {
                post.getMedia().add(PostMedia.builder()
                        .post(post)
                        .media(asset)
                        .position(index)
                        .build());
            } else {
                item.setPosition(index);
            }
        }
    }
}
