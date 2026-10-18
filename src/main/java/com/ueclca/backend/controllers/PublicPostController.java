package com.ueclca.backend.controllers;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ueclca.backend.dtos.PageResponse;
import com.ueclca.backend.dtos.PostResponse;
import com.ueclca.backend.services.PostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private final PostService postService;

    @GetMapping
    public PageResponse<PostResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(postService.searchPublished(pageable), PostResponse::from);
    }

    @GetMapping("/{slug}")
    public PostResponse getBySlug(@PathVariable String slug) {
        return PostResponse.from(postService.getPublishedBySlug(slug));
    }
}
