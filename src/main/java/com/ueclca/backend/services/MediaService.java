package com.ueclca.backend.services;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ueclca.backend.config.MediaProperties;
import com.ueclca.backend.exceptions.BadRequestException;
import com.ueclca.backend.exceptions.ResourceNotFoundException;
import com.ueclca.backend.models.MediaAsset;
import com.ueclca.backend.models.User;
import com.ueclca.backend.repositories.MediaAssetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorage mediaStorage;
    private final MediaProperties mediaProperties;
    private final UserService userService;

    @Transactional
    public MediaAsset upload(MultipartFile file, String uploaderEmail) {
        User uploader = userService.getByEmail(uploaderEmail);
        String storageKey = mediaStorage.store(file);
        String url = publicUrl(storageKey);
        if (url.length() > 500) {
            throw new BadRequestException("La URL del archivo es demasiado larga");
        }

        MediaAsset asset = MediaAsset.builder()
                .storageKey(storageKey)
                .url(url)
                .mimeType(normalizeContentType(file.getContentType()))
                .sizeBytes(file.getSize())
                .uploadedBy(uploader)
                .build();
        return mediaAssetRepository.save(asset);
    }

    public MediaAsset getByStorageKey(String storageKey) {
        return mediaAssetRepository.findByStorageKey(storageKey)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));
    }

    private String publicUrl(String storageKey) {
        String baseUrl = mediaProperties.publicBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/api/v1/media/files/" + storageKey;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
