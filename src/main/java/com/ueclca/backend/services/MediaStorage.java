package com.ueclca.backend.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorage {

    String store(MultipartFile file);

    Resource load(String storageKey);
}
