package com.ueclca.backend.controllers;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ueclca.backend.dtos.MediaResponse;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.services.MediaService;
import com.ueclca.backend.services.MediaStorage;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final MediaStorage mediaStorage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public MediaResponse upload(@RequestPart("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
        return MediaResponse.from(mediaService.upload(file, jwt.getSubject()));
    }

    @GetMapping("/files/{storageKey}")
    public ResponseEntity<Resource> get(@PathVariable String storageKey) {
        MediaAsset asset = mediaService.getByStorageKey(storageKey);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getMimeType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(mediaStorage.load(storageKey));
    }
}
