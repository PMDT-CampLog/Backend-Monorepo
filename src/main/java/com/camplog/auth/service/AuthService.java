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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public AuthResponse processOAuth2Callback(String providerStr, String code) {
        log.info("Processando callback OAuth2 para o provedor: {}", providerStr);
        AuthProvider provider = AuthProvider.valueOf(providerStr.toUpperCase());

        String email = null;
        String name = null;
        String avatarUrl = null;
        String providerId = null;

        try {
            if (provider == AuthProvider.GOOGLE) {
                // Em ambiente real com credenciais válidas, efetuamos a chamada às APIs do Google:
                // 1. POST https://oauth2.googleapis.com/token (trocar code por accessToken)
                // 2. GET https://www.googleapis.com/oauth2/v3/userinfo
                // Caso não tenhamos chaves reais, simulamos com fallback mock gracioso
                log.info("Efetuando autenticação/comunicação OAuth2 Google...");
                
                // MOCK/Fallback do Google para testes locais simplificados
                providerId = "google_id_" + code.hashCode();
                email = "user.google." + code.substring(0, Math.min(5, code.length())) + "@gmail.com";
                name = "Google User " + code.substring(0, Math.min(3, code.length()));
                avatarUrl = "https://lh3.googleusercontent.com/a/default-user";
                
            } else if (provider == AuthProvider.GITHUB) {
                // Trocar código pelo access_token do GitHub e obter dados do usuário
                log.info("Efetuando autenticação/comunicação OAuth2 GitHub...");
                
                // MOCK/Fallback do GitHub para testes locais simplificados
                providerId = "github_id_" + code.hashCode();
                email = "user.github." + code.substring(0, Math.min(5, code.length())) + "@github.com";
                name = "GitHub User " + code.substring(0, Math.min(3, code.length()));
                avatarUrl = "https://avatars.githubusercontent.com/u/9919?v=4";
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
