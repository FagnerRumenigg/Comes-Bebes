package org.application.service.storage;

import org.application.service.exception.InvalidOperationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;

/**
 * Baixa uma imagem de uma URL http(s) pra memória, com as mesmas proteções em
 * qualquer implementação de ImageStorage: bloqueia URLs que não sejam HTTP/HTTPS
 * e endereços de rede interna (SSRF), limita o tamanho e o tempo de espera.
 */
final class RemoteImageDownloader {

    private static final int COPY_BUFFER_SIZE = 1024 * 1024;

    private RemoteImageDownloader() {
    }

    static byte[] download(String sourceUrl, long maxBytes, int connectTimeoutMs, int readTimeoutMs) {
        URI source = URI.create(sourceUrl);
        if (!"http".equalsIgnoreCase(source.getScheme()) && !"https".equalsIgnoreCase(source.getScheme())) {
            throw new InvalidOperationException("A imagem deve usar uma URL HTTP ou HTTPS.");
        }
        if (source.getHost() == null || isPrivateAddress(source.getHost())) {
            throw new InvalidOperationException("A URL da imagem não pode apontar para uma rede interna.");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) source.toURL().openConnection();
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new InvalidOperationException("A URL da imagem não está disponível.");
            }
            try (InputStream inputStream = connection.getInputStream()) {
                return readWithLimit(inputStream, maxBytes);
            }
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidOperationException("Não foi possível baixar a imagem.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readWithLimit(InputStream inputStream, long maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[COPY_BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new InvalidOperationException("A imagem excede o limite de 20 MB.");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static boolean isPrivateAddress(String host) {
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
