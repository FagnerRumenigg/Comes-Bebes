package org.application.service.storage;

import lombok.RequiredArgsConstructor;
import org.application.dto.StoredImage;
import org.application.service.exception.InvalidOperationException;
import org.application.service.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import org.w3c.dom.Node;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_IMAGE_WIDTH = 3840;
    private static final int MAX_IMAGE_HEIGHT = 2160;
    private static final long MAX_IMAGE_PIXELS = (long) MAX_IMAGE_WIDTH * MAX_IMAGE_HEIGHT;

    @Value("${app.storage.local.path}")
    private Path storagePath;

    @Override
    public StoredImage store(String sourceUrl) {
        Path temporaryFile = null;
        try {
            Files.createDirectories(storagePath);
            byte[] content = RemoteImageDownloader.download(sourceUrl, MAX_IMAGE_BYTES, 5000, 15000);
            temporaryFile = Files.createTempFile(storagePath, "upload-", ".tmp");
            Files.write(temporaryFile, content);
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
    public byte[] read(String objectName) {
        Path storageRoot = storagePath.toAbsolutePath().normalize();
        Path target = storageRoot.resolve(objectName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Imagem não encontrada.");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "Imagem não encontrada.");
        }
    }

    /**
     * Recebe bytes já normalizados (WebP) pelo image-validator Python e apenas os persiste.
     * Nenhuma interpretação de EXIF, rotação, resize ou conversão de formato acontece aqui:
     * o Python é a única fonte de verdade do processamento da imagem para o fluxo de upload.
     */
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
            image = ImageIO.read(new java.io.ByteArrayInputStream(content));
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
        try {
            Files.createDirectories(storagePath);
            String objectName = "images/" + UUID.randomUUID() + ".webp";
            Path storageRoot = storagePath.toAbsolutePath().normalize();
            Path target = storageRoot.resolve(objectName).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new InvalidOperationException("Caminho de imagem inválido.");
            }
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return StoredImage.builder()
                    .bucket("comesebebes-local-images")
                    .objectName(objectName)
                    .generation(1L)
                    .format("webp")
                    .sizeBytes((long) content.length)
                    .width(width)
                    .height(height)
                    .build();
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidOperationException("Não foi possível armazenar a imagem localmente.");
        }
    }

    private StoredImage storeValidatedFile(Path sourcePath, String originalFilename) throws IOException {
        long size = Files.size(sourcePath);
        if (size == 0 || size > MAX_IMAGE_BYTES) {
            throw new InvalidOperationException("A imagem excede o limite de 20 MB.");
        }
        BufferedImage image = ImageIO.read(sourcePath.toFile());
        if (image == null) {
            throw new InvalidOperationException("O arquivo informado não é uma imagem válida.");
        }
        image = applyExifOrientation(sourcePath, image);
        image = normalizeDimensions(image);
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
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

    private BufferedImage applyExifOrientation(Path sourcePath, BufferedImage image) throws IOException {
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(sourcePath.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
            if (!readers.hasNext()) {
                return image;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(inputStream, true, true);
                int orientation = 1;
                try {
                    var metadata = reader.getImageMetadata(0);
                    if (metadata != null) {
                        var tree = metadata.getAsTree("javax_imageio_jpeg_image_1.0");
                        if (tree != null) {
                            Node child = tree.getFirstChild();
                            while (child != null) {
                                if ("orientation".equals(child.getNodeName())) {
                                    var value = child.getAttributes().getNamedItem("value");
                                    if (value != null) {
                                        orientation = Integer.parseInt(value.getNodeValue());
                                    }
                                    break;
                                }
                                child = child.getNextSibling();
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                switch (orientation) {
                    case 2 -> {
                        AffineTransform transform = AffineTransform.getScaleInstance(-1, 1);
                        transform.translate(-image.getWidth(), 0);
                        return transformImage(image, transform);
                    }
                    case 3 -> {
                        AffineTransform transform = AffineTransform.getScaleInstance(-1, -1);
                        transform.translate(-image.getWidth(), -image.getHeight());
                        return transformImage(image, transform);
                    }
                    case 4 -> {
                        AffineTransform transform = AffineTransform.getScaleInstance(1, -1);
                        transform.translate(0, -image.getHeight());
                        return transformImage(image, transform);
                    }
                    case 5 -> {
                        AffineTransform flip = AffineTransform.getScaleInstance(-1, 1);
                        flip.translate(-image.getWidth(), 0);
                        return rotate90(transformImage(image, flip));
                    }
                    case 6 -> {
                        return rotate90(image);
                    }
                    case 7 -> {
                        AffineTransform flip = AffineTransform.getScaleInstance(-1, 1);
                        flip.translate(-image.getWidth(), 0);
                        return rotate270(transformImage(image, flip));
                    }
                    case 8 -> {
                        return rotate270(image);
                    }
                    default -> {
                        return image;
                    }
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage transformImage(BufferedImage image, AffineTransform transform) {
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        return op.filter(image, null);
    }

    private BufferedImage rotate90(BufferedImage image) {
        return rotate(image, 90);
    }

    private BufferedImage rotate270(BufferedImage image) {
        return rotate(image, 270);
    }

    private BufferedImage rotate(BufferedImage image, double degrees) {
        BufferedImage rotated = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
        Graphics2D graphics = rotated.createGraphics();
        try {
            graphics.translate((rotated.getWidth() - image.getWidth()) / 2.0, (rotated.getHeight() - image.getHeight()) / 2.0);
            graphics.rotate(Math.toRadians(degrees), image.getWidth() / 2.0, image.getHeight() / 2.0);
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rotated;
    }

    private BufferedImage normalizeDimensions(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT
                && (long) width * height <= MAX_IMAGE_PIXELS) {
            return image;
        }

        double scale = 1.0;
        if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
            double widthScale = (double) MAX_IMAGE_WIDTH / width;
            double heightScale = (double) MAX_IMAGE_HEIGHT / height;
            scale = Math.min(widthScale, heightScale);
        } else if ((long) width * height > MAX_IMAGE_PIXELS) {
            scale = Math.sqrt((double) MAX_IMAGE_PIXELS / (width * height));
        }

        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

}
