package org.application.service.storage;

import org.application.dto.StoredImage;

public interface ImageStorage {

    StoredImage store(String sourceUrl);

    StoredImage store(byte[] content, String originalFilename, String contentType);
}
