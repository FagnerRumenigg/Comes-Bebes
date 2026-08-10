package org.application.dto;

import lombok.Builder;

@Builder
public record StoredImage(
        String bucket,
        String objectName,
        long generation,
        String format,
        long sizeBytes,
        int width,
        int height
) {
}
