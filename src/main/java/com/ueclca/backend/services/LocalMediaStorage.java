package com.ueclca.backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ueclca.backend.config.MediaProperties;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocalMediaStorage implements MediaStorage {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "video/mp4", "mp4");

    private final MediaProperties mediaProperties;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }

        String contentType = normalizeContentType(file.getContentType());
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new BadRequestException("Tipo de archivo no permitido: " + contentType);
        }
        if (file.getSize() > mediaProperties.maxFileSizeBytes()) {
            throw new BadRequestException(
                    "El archivo supera el tamaño máximo permitido de "
                            + mediaProperties.maxFileSizeBytes() + " bytes");
        }

        String storageKey = UUID.randomUUID() + "." + extension;
        Path directory = storageDirectory();
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, directory.resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el archivo", ex);
        }
        return storageKey;
    }

    @Override
    public Resource load(String storageKey) {
        Path directory = storageDirectory();
        Path file = directory.resolve(storageKey).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("Archivo no encontrado");
        }
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("No se pudo leer el archivo", ex);
        }
    }

    private Path storageDirectory() {
        return mediaProperties.storagePath().toAbsolutePath().normalize();
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
