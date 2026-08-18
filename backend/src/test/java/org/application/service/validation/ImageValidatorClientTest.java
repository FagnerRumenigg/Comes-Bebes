package org.application.service.validation;

import org.application.service.exception.InvalidOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O RestTemplate padrão (sem factory customizada) não tem connect/read timeout —
 * bloqueia indefinidamente. Isso passou a importar de verdade com o validador
 * rodando minReplicas=0 no Azure: um cold start (ou uma falha real) não pode
 * travar a thread da API pra sempre.
 */
class ImageValidatorClientTest {

    private ImageValidatorClient buildClient(int port, int connectTimeoutMs, int readTimeoutMs) {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        ImageValidatorClient client = new ImageValidatorClient(objectMapper);
        ReflectionTestUtils.setField(client, "validatorUrl", "http://127.0.0.1:" + port);
        ReflectionTestUtils.setField(client, "connectTimeoutMs", connectTimeoutMs);
        ReflectionTestUtils.setField(client, "readTimeoutMs", readTimeoutMs);
        ReflectionTestUtils.invokeMethod(client, "initRestTemplate");
        return client;
    }

    private int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    void shouldFailFastInsteadOfHangingForeverWhenValidatorNeverResponds() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            CountDownLatch accepted = new CountDownLatch(1);
            Thread serverThread = new Thread(() -> {
                try (Socket socket = serverSocket.accept()) {
                    accepted.countDown();
                    // Aceita a conexão mas nunca escreve resposta — simula um
                    // cold start (ou uma falha) que trava, sem o read-timeout.
                    Thread.sleep(5_000);
                } catch (Exception ignored) {
                    // Socket fechado pelo try-with-resources externo ao final do teste.
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            ImageValidatorClient client = buildClient(port, 2_000, 300);

            long start = System.nanoTime();
            assertThatThrownBy(() -> client.validate(new byte[]{1, 2, 3}, "dish.png", "image/png"))
                    .isInstanceOf(InvalidOperationException.class)
                    .hasFieldOrPropertyWithValue("code", "IMAGE_VALIDATOR_UNAVAILABLE");
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(accepted.await(0, TimeUnit.SECONDS)).isTrue();
            // O servidor "travaria" por 5s; com o read-timeout de 300ms a chamada
            // precisa retornar bem antes disso.
            assertThat(elapsedMs).isLessThan(4_000);
        }
    }

    @Test
    void shouldFailFastWhenNothingIsListeningOnThePort() throws Exception {
        ImageValidatorClient client = buildClient(freePort(), 500, 500);

        assertThatThrownBy(() -> client.validate(new byte[]{1, 2, 3}, "dish.png", "image/png"))
                .isInstanceOf(InvalidOperationException.class)
                .hasFieldOrPropertyWithValue("code", "IMAGE_VALIDATOR_UNAVAILABLE");
    }
}
