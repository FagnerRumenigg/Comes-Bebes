package org.application.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Só existe quando app.email.delivery-mode=azure — em "log" (padrão local),
 * EmailSender nem tenta injetar esse bean (ver ObjectProvider em EmailSender).
 * Mesmo recurso Azure Communication Services usado no Entertain-Me.
 */
@Configuration
@ConditionalOnProperty(name = "app.email.delivery-mode", havingValue = "azure")
public class AzureEmailConfig {
    @Value("${azure.communication.email.endpoint}")
    private String endpoint;

    @Value("${azure.communication.email.access-key}")
    private String accessKey;

    @Bean
    public EmailClient azureEmailClient() {
        return new EmailClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(accessKey))
                .httpClient(new OkHttpAsyncHttpClientBuilder().build())
                .buildClient();
    }
}
