package com.ueclca.backend.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ueclca.backend.dtos.PageResponse;
import com.ueclca.backend.dtos.PostRequest;
import com.ueclca.backend.dtos.PostResponse;
import com.ueclca.backend.dtos.StatusRequest;
import com.ueclca.backend.models.PublicationStatus;
import com.ueclca.backend.services.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class PostController {

    private final PostService postService;

    @GetMapping
    public PageResponse<PostResponse> list(
            @RequestParam(required = false) PublicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(postService.search(status, pageable), PostResponse::from);
    }

    @GetMapping("/{id}")
    public PostResponse get(@PathVariable Long id) {
        return PostResponse.from(postService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request, @AuthenticationPrincipal Jwt jwt) {
        return PostResponse.from(postService.create(request, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return PostResponse.from(postService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public PostResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return PostResponse.from(postService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}
