package com.ueclca.backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ueclca.backend.config.EnrollmentProperties;
import com.ueclca.backend.config.MediaProperties;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentDocumentStorage {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final MediaProperties mediaProperties;
    private final EnrollmentProperties enrollmentProperties;

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }

        String contentType = normalizeContentType(file.getContentType());
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new BadRequestException(
                    "Tipo de archivo no permitido. Adjunta PDF, JPG, PNG o WebP");
        }
        if (file.getSize() > enrollmentProperties.maxDocumentSizeBytes()) {
            throw new BadRequestException(
                    "El archivo supera el tamaño máximo permitido de "
                            + enrollmentProperties.maxDocumentSizeBytes() + " bytes");
        }

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String storageKey = UUID.randomUUID() + "." + extension;
        Path directory = storageDirectory();
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, directory.resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("No se pudo guardar el documento de inscripción {} en {}", storageKey, directory, ex);
            throw new IllegalStateException("No se pudo guardar el archivo", ex);
        }
        return new StoredFile(storageKey, fileName, contentType, file.getSize());
    }

    public Resource load(String storageKey) {
        Path directory = storageDirectory();
        Path file = directory.resolve(storageKey).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException ex) {
            log.error("No se pudo leer el documento de inscripción {}", storageKey, ex);
            throw new IllegalStateException("No se pudo leer el archivo", ex);
        }
    }

    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        Path directory = storageDirectory();
        Path file = directory.resolve(storageKey).normalize();
        return file.startsWith(directory) && Files.isRegularFile(file);
    }

    private Path storageDirectory() {
        return mediaProperties.storagePath().toAbsolutePath().normalize().resolve("inscripciones");
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "documento";
        }
        String fileName = Paths.get(originalFileName).getFileName().toString().trim();
        if (fileName.length() > 255) {
            fileName = fileName.substring(fileName.length() - 255);
        }
        return fileName;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String storageKey, String fileName, String mimeType, long sizeBytes) {
    }
}
