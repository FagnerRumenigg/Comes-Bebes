package org.application.service.storage;

import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import jakarta.annotation.PostConstruct;
import org.application.dto.StoredImage;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Implementação de ImageStorage para Azure Blob Storage, autenticada via Managed
 * Identity (sem chave/connection string guardada). Ativa quando
 * app.storage.type=blob; caso contrário LocalImageStorage continua sendo usada
 * (dev local não precisa de conta Azure pra rodar).
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "blob")
public class BlobImageStorage implements ImageStorage {
    private static final Logger log = LoggerFactory.getLogger(BlobImageStorage.class);

    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_IMAGE_WIDTH = 3840;
    private static final int MAX_IMAGE_HEIGHT = 2160;
    private static final long MAX_IMAGE_PIXELS = (long) MAX_IMAGE_WIDTH * MAX_IMAGE_HEIGHT;

    @Value("${app.storage.blob.account-url}")
    private String accountUrl;

    @Value("${app.storage.blob.container}")
    private String containerName;

    // DefaultAzureCredential precisa saber qual identidade usar quando o
    // Container App tem uma User Assigned Identity (não System Assigned) —
    // sem isso, a resolução da Managed Identity falha ("Unable to load the
    // proper Managed Identity"). Vazio (dev local com outra forma de auth,
    // ou System Assigned) é um valor válido.
    @Value("${app.storage.blob.identity-client-id:}")
    private String identityClientId;

    private BlobContainerClient containerClient;

    @PostConstruct
    void init() {
        DefaultAzureCredentialBuilder credentialBuilder = new DefaultAzureCredentialBuilder();
        if (identityClientId != null && !identityClientId.isBlank()) {
            credentialBuilder.managedIdentityClientId(identityClientId);
        }
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .endpoint(accountUrl)
                .credential(credentialBuilder.build())
                .buildClient();
        containerClient = serviceClient.getBlobContainerClient(containerName);
    }

    /**
     * Caminho legado (POST /publications em JSON, oculto no Swagger, sem uso real
     * hoje — o fluxo atual sempre envia bytes já processados pelo validador
     * Python). Baixa a URL e delega pro mesmo armazenamento do fluxo principal,
     * sem reprocessar EXIF/rotação/resize: essa normalização só existe hoje no
     * LocalImageStorage, mantida lá por paridade histórica com este endpoint
     * oculto, não porque o fluxo real de upload precise dela.
     */
    @Override
    public StoredImage store(String sourceUrl) {
        byte[] content = RemoteImageDownloader.download(sourceUrl, MAX_IMAGE_BYTES, 5000, 15000);
        String filename = URI.create(sourceUrl).getPath();
        return store(content, filename == null ? "upload.img" : filename, null);
    }

    @Override
    public StoredImage store(byte[] content, String originalFilename, String contentType) {
        if (content == null || content.length == 0) {
            throw new InvalidOperationException("A imagem não pode estar vazia.");
        }
        if (content.length > MAX_IMAGE_BYTES) {
            throw new InvalidOperationException("A imagem excede o limite de 20 MB.");
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(content));
        } catch (IOException exception) {
            throw new InvalidOperationException("O arquivo informado não é uma imagem válida.");
        }
        if (image == null) {
            throw new InvalidOperationException("O arquivo informado não é uma imagem válida.");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0 || width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT
                || (long) width * height > MAX_IMAGE_PIXELS) {
            throw new InvalidOperationException("As dimensões da imagem excedem o limite permitido.");
        }

        String objectName = "images/" + UUID.randomUUID() + ".webp";
        try {
            BlobClient blobClient = containerClient.getBlobClient(objectName);
            blobClient.upload(BinaryData.fromBytes(content));
            blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType("image/webp"));
        } catch (BlobStorageException exception) {
            log.warn("event=blob_upload_failed objectName={}", objectName, exception);
            throw new InvalidOperationException("Não foi possível armazenar a imagem.");
        }

        return StoredImage.builder()
                .bucket(containerName)
                .objectName(objectName)
                .generation(1L)
                .format("webp")
                .sizeBytes((long) content.length)
                .width(width)
                .height(height)
                .build();
    }

    @Override
    public byte[] read(String objectName) {
        try {
            return containerClient.getBlobClient(objectName).downloadContent().toBytes();
        } catch (BlobStorageException exception) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Imagem não encontrada.");
        }
    }
}
