package org.application.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Em app.email.delivery-mode=log (padrão local) só loga, sem exigir
 * credencial nenhuma; em "azure" manda de verdade via Azure Communication
 * Services Email — mesmo padrão do Entertain-Me.
 */
@Component
public class EmailSender {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ObjectProvider<EmailClient> emailClientProvider;
    private final ResourceLoader resourceLoader;
    private final String deliveryMode;
    private final String senderAddress;

    public EmailSender(
            ObjectProvider<EmailClient> emailClientProvider,
            ResourceLoader resourceLoader,
            @Value("${app.email.delivery-mode}") String deliveryMode,
            @Value("${app.email.sender-address}") String senderAddress
    ) {
        this.emailClientProvider = emailClientProvider;
        this.resourceLoader = resourceLoader;
        this.deliveryMode = deliveryMode;
        this.senderAddress = senderAddress;
    }

    public void sendPasswordReset(String toEmail, String resetLink) {
        String html = renderTemplate("password-reset.html", resetLink);
        String plainText = "Recebemos um pedido para trocar sua senha no Comes&Bebes. "
                + "Acesse o link a seguir para criar uma senha nova — ele vale por 1 hora:\n"
                + resetLink
                + "\n\nSe você não pediu isso, pode ignorar este e-mail.";
        send(toEmail, "Trocar sua senha no Comes&Bebes", plainText, html);
    }

    private void send(String toEmail, String subject, String plainTextBody, String htmlBody) {
        if (!"azure".equalsIgnoreCase(deliveryMode)) {
            log.warn("event=email_log_only to={} subject={} body={}", toEmail, subject, plainTextBody);
            return;
        }

        EmailClient emailClient = emailClientProvider.getIfAvailable();
        if (emailClient == null) {
            throw new IllegalStateException("Cliente de e-mail do Azure indisponível com delivery-mode=azure.");
        }

        EmailMessage message = new EmailMessage()
                .setSenderAddress(senderAddress)
                .setSubject(subject)
                .setBodyPlainText(plainTextBody)
                .setBodyHtml(htmlBody)
                .setToRecipients(toEmail);

        SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(message);
        PollResponse<EmailSendResult> response = poller.waitForCompletion();
        log.info("event=email_sent to={} subject={} id={}", toEmail, subject, response.getValue().getId());
    }

    private String renderTemplate(String templateName, String link) {
        try {
            var resource = resourceLoader.getResource("classpath:templates/email/" + templateName);
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return template.replace("{{LINK}}", link);
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível carregar o template de e-mail: " + templateName, exception);
        }
    }
}
