package org.application.service.storage;

import lombok.RequiredArgsConstructor;
import org.application.dto.StoredImage;
import org.application.service.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Primary
@Component
@RequiredArgsConstructor
public class LocalImageStorage implements ImageStorage {

    private static final long MAX_IMAGE_BYTES = 15L * 1024 * 1024;
    private static final int MAX_IMAGE_WIDTH = 3840;
    private static final int MAX_IMAGE_HEIGHT = 2160;
    private static final long MAX_IMAGE_PIXELS = (long) MAX_IMAGE_WIDTH * MAX_IMAGE_HEIGHT;
    private static final int COPY_BUFFER_SIZE = 1024 * 1024;

    @Value("${app.storage.local.path}")
    private Path storagePath;

    @Override
    public StoredImage store(String sourceUrl) {
        Path temporaryFile = null;
        try {
            Files.createDirectories(storagePath);
            temporaryFile = Files.createTempFile(storagePath, "upload-", ".tmp");
            copySource(sourceUrl, temporaryFile);
            String filename = URI.create(sourceUrl).getPath();
            return storeValidatedFile(temporaryFile, filename == null ? "upload.img" : filename);
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidOperationException("Não foi possível armazenar a imagem localmente.");
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public StoredImage store(byte[] content, String originalFilename, String contentType) {
        if (content == null || content.length == 0) {
            throw new InvalidOperationException("A imagem não pode estar vazia.");
        }
        if (content.length > MAX_IMAGE_BYTES) {
            throw new InvalidOperationException("A imagem excede o limite de 15 MB.");
        }
        try {
            Files.createDirectories(storagePath);
            Path temporaryFile = Files.createTempFile(storagePath, "upload-", ".tmp");
            Files.write(temporaryFile, content);
            try {
                return storeValidatedFile(temporaryFile, originalFilename == null ? "upload.img" : originalFilename);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidOperationException("Não foi possível armazenar a imagem localmente.");
        }
    }

    private void copySource(String sourceUrl, Path target) throws IOException {
        URI source = URI.create(sourceUrl);
        if (!"http".equalsIgnoreCase(source.getScheme()) && !"https".equalsIgnoreCase(source.getScheme())) {
            throw new InvalidOperationException("A imagem deve usar uma URL HTTP ou HTTPS.");
        }
        if (source.getHost() == null || isPrivateAddress(source.getHost())) {
            throw new InvalidOperationException("A URL da imagem não pode apontar para uma rede interna.");
        }
        HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(false);
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            throw new InvalidOperationException("A URL da imagem não está disponível.");
        }
        try (InputStream inputStream = connection.getInputStream()) {
            copyWithLimit(inputStream, target);
        } finally {
            connection.disconnect();
        }
    }

    private StoredImage storeValidatedFile(Path sourcePath, String originalFilename) throws IOException {
        long size = Files.size(sourcePath);
        if (size == 0 || size > MAX_IMAGE_BYTES) {
            throw new InvalidOperationException("A imagem excede o limite de 15 MB.");
        }
        BufferedImage image = ImageIO.read(sourcePath.toFile());
        if (image == null) {
            throw new InvalidOperationException("O arquivo informado não é uma imagem válida.");
        }
        if (image.getWidth() <= 0 || image.getHeight() <= 0
                || image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT
                || (long) image.getWidth() * image.getHeight() > MAX_IMAGE_PIXELS) {
            throw new InvalidOperationException("As dimensões da imagem excedem o limite permitido.");
        }

        String format = "webp";
        String objectName = "images/" + UUID.randomUUID() + ".webp";
        Path storageRoot = storagePath.toAbsolutePath().normalize();
        Path target = storageRoot.resolve(objectName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new InvalidOperationException("Caminho de imagem inválido.");
        }
        Files.createDirectories(target.getParent());
        if (!ImageIO.write(image, "webp", target.toFile())) {
            throw new InvalidOperationException("Não foi possível normalizar a imagem para WebP.");
        }
        return StoredImage.builder()
                .bucket("comesebebes-local-images")
                .objectName(objectName)
                .generation(1L)
                .format(format)
                .sizeBytes(Files.size(target))
                .width(image.getWidth())
                .height(image.getHeight())
                .build();
    }

    private void copyWithLimit(InputStream inputStream, Path target) throws IOException {
        try (var outputStream = Files.newOutputStream(target)) {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMAGE_BYTES) {
                    throw new InvalidOperationException("A imagem excede o limite de 15 MB.");
                }
                outputStream.write(buffer, 0, read);
            }
        }
    }

    private boolean isPrivateAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            return true;
        }
    }
}
