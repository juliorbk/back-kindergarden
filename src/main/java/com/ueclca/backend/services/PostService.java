package com.ueclca.backend.services;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Post;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.PostRepository;
import com.ueclca.backend.utils.SlugUtils;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;

    public PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository;
        this.userService = userService;
    }

    public Page<Post> search(PublicationStatus status, Pageable pageable) {
        if (status == null) {
            return postRepository.findAll(pageable);
        }
        return postRepository.findByStatus(status, pageable);
    }

    public Post getById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));
    }

    @Transactional
    public Post create(PostRequest request, String authorEmail) {
        User author = userService.getByEmail(authorEmail);
        String slug = SlugUtils.uniqueSlug(request.title(), postRepository::existsBySlug);

        Post post = Post.builder()
                .slug(slug)
                .title(request.title().trim())
                .caption(request.caption().trim())
                .imageUrl(request.imageUrl().trim())
                .status(PublicationStatus.DRAFT)
                .author(author)
                .build();

        return postRepository.save(post);
    }

    @Transactional
    public Post update(Long id, PostRequest request) {
        Post post = getById(id);
        post.setTitle(request.title().trim());
        post.setCaption(request.caption().trim());
        post.setImageUrl(request.imageUrl().trim());
        return post;
    }

    @Transactional
    public Post changeStatus(Long id, PublicationStatus status) {
        Post post = getById(id);
        if (post.getStatus() != status) {
            if (status == PublicationStatus.PUBLISHED && post.getPublishedAt() == null) {
                post.setPublishedAt(Instant.now());
            }
            post.setStatus(status);
        }
        return post;
    }

    @Transactional
    public void delete(Long id) {
        postRepository.delete(getById(id));
    }

    public Page<Post> searchPublished(Pageable pageable) {
        Pageable sortedByPublishedAt = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        return postRepository.findByStatus(PublicationStatus.PUBLISHED, sortedByPublishedAt);
    }

    public Post getPublishedBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));
        if (post.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Publicación no encontrada");
        }
        return post;
    }
}
