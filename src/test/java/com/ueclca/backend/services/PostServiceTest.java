package com.ueclca.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.Channel;
import com.ueclca.backend.models.Post;
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
}
