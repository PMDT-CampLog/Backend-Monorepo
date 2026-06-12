package com.camplog.auth.service;

import com.camplog.auth.config.JwtService;
import com.camplog.auth.dto.AuthResponse;
import com.camplog.auth.dto.LoginRequest;
import com.camplog.auth.dto.RegisterRequest;
import com.camplog.auth.event.UserCreatedEvent;
import com.camplog.auth.model.AuthProvider;
import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.oauth2.github.client-id:}")
    private String githubClientId;

    @Value("${app.oauth2.github.client-secret:}")
    private String githubClientSecret;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String defaultRedirectUri;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Tentativa de cadastro tradicional para e-mail: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este e-mail já está em uso.");
        }

        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("member")
                .provider(AuthProvider.LOCAL)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Usuário cadastrado com ID: {}", savedUser.getId());

        // Dispara o evento de criação de usuário para as tarefas de segundo plano
        eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser));

        String token = jwtService.generateToken(savedUser);
        return buildAuthResponse(token, savedUser);
    }

    @Transactional
    public AuthResponse processOAuth2Callback(String providerStr, String code, String clientRedirectUri) {
        log.info("Processando callback OAuth2 para o provedor: {}", providerStr);
        AuthProvider provider = AuthProvider.valueOf(providerStr.toUpperCase());

        String email = null;
        String name = null;
        String avatarUrl = null;
        String providerId = null;

        try {
            if (provider == AuthProvider.GOOGLE) {
                if (googleClientId != null && !googleClientId.trim().isEmpty() &&
                    googleClientSecret != null && !googleClientSecret.trim().isEmpty()) {
                    
                    log.info("Efetuando autenticação REAL com o Google...");
                    String redirect = (clientRedirectUri != null && !clientRedirectUri.trim().isEmpty()) ? clientRedirectUri : defaultRedirectUri;
                    
                    String form = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                            "&client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8) +
                            "&client_secret=" + URLEncoder.encode(googleClientSecret, StandardCharsets.UTF_8) +
                            "&redirect_uri=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8) +
                            "&grant_type=authorization_code";
                            
                    HttpRequest tokenRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://oauth2.googleapis.com/token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(form))
                            .build();
                            
                    HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
                    if (tokenResponse.statusCode() != 200) {
                        log.error("Erro ao obter token do Google. Status: {}, Body: {}", tokenResponse.statusCode(), tokenResponse.body());
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Erro na comunicação com o Google.");
                    }
                    
                    Map<String, Object> tokenMap = objectMapper.readValue(tokenResponse.body(), Map.class);
                    String accessToken = (String) tokenMap.get("access_token");
                    
                    HttpRequest userRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();
                            
                    HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
                    if (userResponse.statusCode() != 200) {
                        log.error("Erro ao obter dados de usuário do Google. Status: {}, Body: {}", userResponse.statusCode(), userResponse.body());
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Erro ao recuperar perfil do Google.");
                    }
                    
                    Map<String, Object> userMap = objectMapper.readValue(userResponse.body(), Map.class);
                    providerId = (String) userMap.get("sub");
                    email = (String) userMap.get("email");
                    name = (String) userMap.get("name");
                    avatarUrl = (String) userMap.get("picture");
                } else {
                    log.warn("GOOGLE_CLIENT_ID ou GOOGLE_CLIENT_SECRET não configurados. Usando mock fallback.");
                    providerId = "google_id_" + code.hashCode();
                    email = "user.google." + code.substring(0, Math.min(5, code.length())) + "@gmail.com";
                    name = "Google User " + code.substring(0, Math.min(3, code.length()));
                    avatarUrl = "https://lh3.googleusercontent.com/a/default-user";
                }
                
            } else if (provider == AuthProvider.GITHUB) {
                if (githubClientId != null && !githubClientId.trim().isEmpty() &&
                    githubClientSecret != null && !githubClientSecret.trim().isEmpty()) {
                    
                    log.info("Efetuando autenticação REAL com o GitHub...");
                    String redirect = (clientRedirectUri != null && !clientRedirectUri.trim().isEmpty()) ? clientRedirectUri : defaultRedirectUri;
                    
                    String form = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                            "&client_id=" + URLEncoder.encode(githubClientId, StandardCharsets.UTF_8) +
                            "&client_secret=" + URLEncoder.encode(githubClientSecret, StandardCharsets.UTF_8) +
                            "&redirect_uri=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8);
                            
                    HttpRequest tokenRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://github.com/login/oauth/access_token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(form))
                            .build();
                            
                    HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
                    if (tokenResponse.statusCode() != 200) {
                        log.error("Erro ao obter token do GitHub. Status: {}, Body: {}", tokenResponse.statusCode(), tokenResponse.body());
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Erro na comunicação com o GitHub.");
                    }
                    
                    Map<String, Object> tokenMap = objectMapper.readValue(tokenResponse.body(), Map.class);
                    String accessToken = (String) tokenMap.get("access_token");
                    
                    HttpRequest userRequest = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.github.com/user"))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Accept", "application/vnd.github.v3+json")
                            .header("User-Agent", "CampLog-Backend")
                            .GET()
                            .build();
                            
                    HttpResponse<String> userResponse = httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());
                    if (userResponse.statusCode() != 200) {
                        log.error("Erro ao obter dados de usuário do GitHub. Status: {}, Body: {}", userResponse.statusCode(), userResponse.body());
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Erro ao recuperar perfil do GitHub.");
                    }
                    
                    Map<String, Object> userMap = objectMapper.readValue(userResponse.body(), Map.class);
                    providerId = String.valueOf(userMap.get("id"));
                    name = (String) userMap.get("name");
                    if (name == null || name.trim().isEmpty()) {
                        name = (String) userMap.get("login");
                    }
                    avatarUrl = (String) userMap.get("avatar_url");
                    email = (String) userMap.get("email");
                    
                    if (email == null || email.trim().isEmpty()) {
                        log.info("E-mail público do GitHub nulo. Buscando e-mail privado do usuário...");
                        HttpRequest emailsRequest = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.github.com/user/emails"))
                                .header("Authorization", "Bearer " + accessToken)
                                .header("Accept", "application/vnd.github.v3+json")
                                .header("User-Agent", "CampLog-Backend")
                                .GET()
                                .build();
                                
                        HttpResponse<String> emailsResponse = httpClient.send(emailsRequest, HttpResponse.BodyHandlers.ofString());
                        if (emailsResponse.statusCode() == 200) {
                            java.util.List<Map<String, Object>> emailsList = objectMapper.readValue(emailsResponse.body(), java.util.List.class);
                            for (Map<String, Object> emailObj : emailsList) {
                                Boolean primary = (Boolean) emailObj.get("primary");
                                if (primary != null && primary) {
                                    email = (String) emailObj.get("email");
                                    break;
                                }
                            }
                            if ((email == null || email.trim().isEmpty()) && !emailsList.isEmpty()) {
                                email = (String) emailsList.get(0).get("email");
                            }
                        }
                    }
                } else {
                    log.warn("GITHUB_CLIENT_ID ou GITHUB_CLIENT_SECRET não configurados. Usando mock fallback.");
                    providerId = "github_id_" + code.hashCode();
                    email = "user.github." + code.substring(0, Math.min(5, code.length())) + "@github.com";
                    name = "GitHub User " + code.substring(0, Math.min(3, code.length()));
                    avatarUrl = "https://avatars.githubusercontent.com/u/9919?v=4";
                }
            }
        } catch (Exception e) {
            log.error("Erro ao obter dados cadastrais do provedor OAuth", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falha na verificação com o provedor OAuth2.");
        }

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível obter o e-mail do perfil social.");
        }

        // Buscar usuário existente ou criar um novo automaticamente
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            log.info("Usuário OAuth existente encontrado. Efetuando login para: {}", email);
            
            // Atualiza informações que possam ter mudado no provedor
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            user.setProviderId(providerId);
            user = userRepository.save(user);
        } else {
            log.info("Novo usuário OAuth2. Criando conta automática para: {}", email);
            user = User.builder()
                    .name(name)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .provider(provider)
                    .providerId(providerId)
                    .role("member")
                    .build();
            user = userRepository.save(user);

            // Dispara evento apenas se for uma nova conta criada por OAuth
            eventPublisher.publishEvent(new UserCreatedEvent(this, user));
        }

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Tentativa de login para e-mail: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "E-mail não cadastrado."));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    @Transactional
    public AuthResponse updateUserRole(User user, String newRole) {
        log.info("Atualizando role do usuário {} para {}", user.getEmail(), newRole);
        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        dbUser.setRole(newRole);
        User savedUser = userRepository.save(dbUser);
        
        String token = jwtService.generateToken(savedUser);
        return buildAuthResponse(token, savedUser);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        String createdAtStr = user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME);
        
        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole())
                        .createdAt(createdAtStr)
                        .build())
                .build();
    }
}
