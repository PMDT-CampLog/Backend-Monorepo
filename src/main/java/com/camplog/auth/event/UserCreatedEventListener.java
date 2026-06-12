package com.camplog.auth.event;

import com.camplog.auth.model.User;
import com.camplog.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventListener {

    private final EmailService emailService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.data-platform.webhook-url}")
    private String dataPlatformUrl;

    @Value("${app.data-platform.secret-token}")
    private String dataPlatformSecret;

    @EventListener
    @Async // Executa de forma totalmente assíncrona para desempenho excepcional do fluxo de resposta REST
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        User user = event.getUser();
        log.info("Processando novo evento USER_CREATED para o usuário: {}", user.getEmail());

        // 1. Enviar e-mail de boas-vindas
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            log.error("Falha ao disparar o e-mail de boas-vindas para o usuário " + user.getEmail(), e);
        }

        // 2. Enviar evento para o Data Platform de forma segura (Excluindo a senha de forma categórica)
        try {
            sendEventToDataPlatform(user);
        } catch (Exception e) {
            log.error("Falha ao despachar evento de cadastro para o Data Platform", e);
        }
    }

    private void sendEventToDataPlatform(User user) {
        log.info("Notificando pipeline do Data Platform em: {}", dataPlatformUrl);

        String createdAtStr = user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME);
        
        // JSON Payload contendo informações cadastrais. Senha NÃO é adicionada ao Payload por razões de LGPD/Segurança
        String jsonPayload = String.format(
                "{\"userId\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"provider\":\"%s\",\"createdAt\":\"%s\"}",
                user.getId(),
                user.getName().replace("\"", "\\\""),
                user.getEmail(),
                user.getProvider().name(),
                createdAtStr
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dataPlatformUrl))
                .header("Content-Type", "application/json")
                .header("X-CampLog-Signature", dataPlatformSecret) // Token de autorização para o webhook
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        log.info("Evento de cadastro ingerido com sucesso no Data Platform! Status: {}", response.statusCode());
                    } else {
                        log.warn("Data Platform retornou status de falha: {}. Response: {}", 
                                response.statusCode(), response.body());
                    }
                })
                .exceptionally(ex -> {
                    log.error("Falha na conexão assíncrona com o Data Platform", ex);
                    return null;
                });
    }
}
