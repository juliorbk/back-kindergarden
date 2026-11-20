package com.ueclca.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ueclca.backend.dtos.EnrollmentDocumentUploadResponse;
import com.ueclca.backend.dtos.EnrollmentRequest;
import com.ueclca.backend.dtos.EnrollmentResponse;
import com.ueclca.backend.services.EnrollmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/public/enrollments")
@RequiredArgsConstructor
public class PublicEnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse create(@Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.createPublic(request);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentDocumentUploadResponse uploadDocument(@RequestPart("file") MultipartFile file) {
        return enrollmentService.storeDocument(file);
    }
}
