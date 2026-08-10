package org.application.service.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.application.service.exception.InvalidOperationException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldStoreImageLocallyAndReturnMetadata() throws Exception {
        Path source = temporaryDirectory.resolve("dish.png");
        BufferedImage image = new BufferedImage(32, 18, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", source.toFile());
        Path storageDirectory = temporaryDirectory.resolve("uploads");

        LocalImageStorage storage = new LocalImageStorage();
        ReflectionTestUtils.setField(storage, "storagePath", storageDirectory);

        var stored = storage.store(Files.readAllBytes(source), "dish.png", "image/png");

        assertThat(stored.bucket()).isEqualTo("comesebebes-local-images");
        assertThat(stored.format()).isEqualTo("webp");
        assertThat(stored.width()).isEqualTo(32);
        assertThat(stored.height()).isEqualTo(18);
        assertThat(stored.sizeBytes()).isGreaterThan(0);
        assertThat(stored.objectName()).endsWith(".webp");
        assertThat(Files.exists(storageDirectory.resolve(stored.objectName()))).isTrue();
    }

    @Test
    void shouldRejectNonImageContent() {
        LocalImageStorage storage = new LocalImageStorage();
        ReflectionTestUtils.setField(storage, "storagePath", temporaryDirectory.resolve("uploads"));

        assertThatThrownBy(() -> storage.store("not-an-image".getBytes(), "dish.png", "image/png"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("imagem válida");
    }

    @Test
    void shouldRejectPrivateFileUrls() {
        LocalImageStorage storage = new LocalImageStorage();
        ReflectionTestUtils.setField(storage, "storagePath", temporaryDirectory.resolve("uploads"));

        assertThatThrownBy(() -> storage.store("file:///etc/passwd"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("HTTP ou HTTPS");
    }

    @Test
    void shouldRejectImageWithExcessiveDimensions() throws Exception {
        Path source = temporaryDirectory.resolve("large.png");
        BufferedImage image = new BufferedImage(3841, 1, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", source.toFile());
        LocalImageStorage storage = new LocalImageStorage();
        ReflectionTestUtils.setField(storage, "storagePath", temporaryDirectory.resolve("uploads"));

        assertThatThrownBy(() -> storage.store(Files.readAllBytes(source), "large.png", "image/png"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("dimensões");
    }
}
