package com.camplog.auth.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/oauth2/authorization")
@Slf4j
public class OAuth2RedirectController {

    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth2.github.client-id:}")
    private String githubClientId;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @GetMapping("/{provider}")
    public ResponseEntity<Void> redirectToProvider(@PathVariable("provider") String provider) {
        String providerLower = provider.toLowerCase();
        log.info("Recebida requisição de autorização OAuth2 para o provedor: {}", providerLower);

        String targetUrl;
        boolean isConfigured = false;

        if ("google".equals(providerLower)) {
            if (googleClientId != null && !googleClientId.trim().isEmpty()) {
                isConfigured = true;
                targetUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                        "?client_id=" + googleClientId +
                        "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                        "&response_type=code" +
                        "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8) +
                        "&state=google";
                log.info("Redirecionando para tela de consentimento REAL do Google...");
            } else {
                log.warn("Chave GOOGLE_CLIENT_ID não configurada. Ativando fallback simulado (Mock).");
                targetUrl = getMockRedirectUrl("google");
            }
        } else if ("github".equals(providerLower)) {
            if (githubClientId != null && !githubClientId.trim().isEmpty()) {
                isConfigured = true;
                targetUrl = "https://github.com/login/oauth/authorize" +
                        "?client_id=" + githubClientId +
                        "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode("user:email read:user", StandardCharsets.UTF_8) +
                        "&state=github";
                log.info("Redirecionando para tela de consentimento REAL do GitHub...");
            } else {
                log.warn("Chave GITHUB_CLIENT_ID não configurada. Ativando fallback simulado (Mock).");
                targetUrl = getMockRedirectUrl("github");
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }

    private String getMockRedirectUrl(String provider) {
        String mockCode = "mock_code_" + UUID.randomUUID().toString().substring(0, 8);
        return "http://localhost:3000/oauth2/callback?provider=" + provider + "&code=" + mockCode;
    }
}
