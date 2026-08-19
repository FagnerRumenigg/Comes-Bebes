package org.application.service.storage;

import org.application.dto.StoredImage;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FakeImageStorage implements ImageStorage {

    @Override
    public StoredImage store(String sourceUrl) {
        return fakeImage();
    }

    @Override
    public StoredImage store(byte[] content, String originalFilename, String contentType) {
        return fakeImage();
    }

    @Override
    public byte[] read(String objectName) {
        return new byte[0];
    }

    private StoredImage fakeImage() {
        return StoredImage.builder()
                .bucket("comesebebes-local-images")
                .objectName("simulated/" + UUID.randomUUID() + ".webp")
                .generation(1L)
                .format("webp")
                .sizeBytes(1024L)
                .width(1200)
                .height(800)
                .build();
    }
}
