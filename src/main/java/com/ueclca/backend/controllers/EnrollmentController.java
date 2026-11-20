package com.ueclca.backend.controllers;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ueclca.backend.dtos.EnrollmentRequest;
import com.ueclca.backend.dtos.EnrollmentResponse;
import com.ueclca.backend.dtos.EnrollmentSummaryResponse;
import com.ueclca.backend.dtos.PageResponse;
import com.ueclca.backend.models.EnrollmentDocumentType;
import com.ueclca.backend.models.GroupName;
import com.ueclca.backend.services.EnrollmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    public PageResponse<EnrollmentSummaryResponse> list(
            @RequestParam(required = false) String schoolYear,
            @RequestParam(required = false) GroupName group,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(
                enrollmentService.search(schoolYear, group, query, pageable),
                Function.identity());
    }

    @GetMapping("/{id}")
    public EnrollmentResponse get(@PathVariable Long id) {
        return enrollmentService.get(id);
    }

    @PutMapping("/{id}")
    public EnrollmentResponse update(@PathVariable Long id, @Valid @RequestBody EnrollmentRequest request) {
        return enrollmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enrollmentService.delete(id);
    }

    @GetMapping("/{id}/documents/{documentType}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @PathVariable EnrollmentDocumentType documentType) {
        EnrollmentService.DocumentDownload download = enrollmentService.loadDocument(id, documentType);
        MediaType mediaType = download.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.mimeType());
        String fileName = download.fileName() == null ? documentType.name() : download.fileName();
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }
}
