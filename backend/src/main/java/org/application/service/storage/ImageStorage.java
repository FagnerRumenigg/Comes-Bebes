package org.application.service.storage;

import org.application.dto.StoredImage;

public interface ImageStorage {

    StoredImage store(String sourceUrl);

    StoredImage store(byte[] content, String originalFilename, String contentType);

    /**
     * Lê os bytes de uma imagem já armazenada (objectName vindo de
     * Publication.gcsObjectName). Lança ResourceNotFoundException se não existir.
     */
    byte[] read(String objectName);
}
