package org.application.service.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlobImageStorageTest {

    private BlobImageStorage buildStorage(BlobContainerClient containerClient, String containerName) {
        BlobImageStorage storage = new BlobImageStorage();
        ReflectionTestUtils.setField(storage, "containerClient", containerClient);
        ReflectionTestUtils.setField(storage, "containerName", containerName);
        return storage;
    }

    private byte[] webpBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ImageIO.write(image, "webp", buffer);
        return buffer.toByteArray();
    }

    @Test
    void shouldRejectNonImageContent() {
        BlobImageStorage storage = buildStorage(mock(BlobContainerClient.class), "comesebebes-images");

        assertThatThrownBy(() -> storage.store("not-an-image".getBytes(), "dish.png", "image/webp"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("imagem válida");
    }

    @Test
    void shouldRejectEmptyContent() {
        BlobImageStorage storage = buildStorage(mock(BlobContainerClient.class), "comesebebes-images");

        assertThatThrownBy(() -> storage.store(new byte[0], "dish.png", "image/webp"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("não pode estar vazia");
    }

    @Test
    void shouldUploadValidImageAndReturnMetadata() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(any())).thenReturn(blobClient);
        BlobImageStorage storage = buildStorage(containerClient, "comesebebes-images");

        byte[] content = webpBytes(32, 18);
        var stored = storage.store(content, "dish.png", "image/webp");

        assertThat(stored.bucket()).isEqualTo("comesebebes-images");
        assertThat(stored.format()).isEqualTo("webp");
        assertThat(stored.width()).isEqualTo(32);
        assertThat(stored.height()).isEqualTo(18);
        assertThat(stored.sizeBytes()).isEqualTo(content.length);
        assertThat(stored.objectName()).startsWith("images/").endsWith(".webp");

        verify(blobClient).upload(any(BinaryData.class));
        verify(blobClient).setHttpHeaders(any(BlobHttpHeaders.class));
    }

    @Test
    void shouldMapUploadFailureToInvalidOperationException() throws Exception {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(any())).thenReturn(blobClient);
        doThrow(mock(BlobStorageException.class)).when(blobClient).upload(any(BinaryData.class));
        BlobImageStorage storage = buildStorage(containerClient, "comesebebes-images");

        assertThatThrownBy(() -> storage.store(webpBytes(10, 10), "dish.png", "image/webp"))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void shouldReadStoredBlobContent() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(eq("images/foo.webp"))).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromBytes(new byte[]{1, 2, 3}));
        BlobImageStorage storage = buildStorage(containerClient, "comesebebes-images");

        assertThat(storage.read("images/foo.webp")).containsExactly(1, 2, 3);
    }

    @Test
    void shouldMapMissingBlobToResourceNotFoundException() {
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(eq("images/missing.webp"))).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenThrow(mock(BlobStorageException.class));
        BlobImageStorage storage = buildStorage(containerClient, "comesebebes-images");

        assertThatThrownBy(() -> storage.read("images/missing.webp"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
