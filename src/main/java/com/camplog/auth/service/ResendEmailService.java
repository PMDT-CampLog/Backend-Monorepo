package com.camplog.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    @Value("${app.email.api-key}")
    private String apiKey;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.provider}")
    private String provider;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    @Async // Executa em background de forma assíncrona para não travar a resposta do cadastro
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "Seja muito bem-vindo ao CampLog, " + userName + "!";
        String htmlContent = "<html>" +
                "<body>" +
                "<h2>Olá " + userName + "!</h2>" +
                "<p>Sua conta na plataforma <strong>CampLog</strong> foi criada com sucesso!</p>" +
                "<p>Agora você pode documentar suas aventuras e compartilhar suas experiências.</p>" +
                "<br>" +
                "<p>Atenciosamente,<br>Equipe CampLog</p>" +
                "</body>" +
                "</html>";

        if ("mock".equalsIgnoreCase(provider) || "mock-key".equalsIgnoreCase(apiKey)) {
            log.info("========== [MOCK EMAIL SENT] ==========");
            log.info("From: {}", fromEmail);
            log.info("To: {}", toEmail);
            log.info("Subject: {}", subject);
            log.info("Content: {}", htmlContent);
            log.info("========================================");
            return;
        }

        try {
            log.info("Disparando e-mail transacional via API do Resend para: {}", toEmail);
            
            // JSON Payload para a API do Resend (https://api.resend.com/emails)
            String jsonPayload = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\"}",
                    fromEmail.replace("\"", "\\\""),
                    toEmail,
                    subject.replace("\"", "\\\""),
                    htmlContent.replace("\"", "\\\"")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.info("E-mail enviado com sucesso via Resend! Status: {}", response.statusCode());
                        } else {
                            log.error("Falha ao enviar e-mail via Resend. Status: {}, Response: {}", 
                                    response.statusCode(), response.body());
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Erro assíncrono ao enviar e-mail via Resend API", ex);
                        return null;
                    });

        } catch (Exception e) {
            log.error("Erro crítico ao construir requisição de e-mail do Resend", e);
        }
    }
}
