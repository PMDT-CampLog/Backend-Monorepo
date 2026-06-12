package com.camplog.auth.controller;

import com.camplog.auth.dto.AuthResponse;
import com.camplog.auth.dto.LoginRequest;
import com.camplog.auth.dto.OAuth2CallbackRequest;
import com.camplog.auth.dto.RegisterRequest;
import com.camplog.auth.model.User;
import com.camplog.auth.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Recebida requisição REST de cadastro tradicional para e-mail: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/oauth2/callback/{provider}")
    public ResponseEntity<AuthResponse> oauthCallback(
            @PathVariable("provider") String provider,
            @Valid @RequestBody OAuth2CallbackRequest request
    ) {
        log.info("Recebida requisição REST de callback OAuth2 para o provedor: {}", provider);
        AuthResponse response = authService.processOAuth2Callback(provider, request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Recebida requisição REST de login tradicional para e-mail: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/role")
    public ResponseEntity<AuthResponse> updateRole(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request
    ) {
        String newRole = request.get("role");
        if (newRole == null || (!newRole.equals("member") && !newRole.equals("creator"))) {
            throw new IllegalArgumentException("Função (role) inválida ou não especificada.");
        }
        log.info("Recebida requisição REST de atualização de role para o usuário {}: {}", user.getEmail(), newRole);
        AuthResponse response = authService.updateUserRole(user, newRole);
        return ResponseEntity.ok(response);
    }

    // --- Tratamento de Erros Padronizado de acordo com o ApiError do Frontend ---

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", ex.getStatusCode().toString());
        errorBody.put("message", ex.getReason());
        return new ResponseEntity<>(errorBody, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "VALIDATION_FAILED");
        errorBody.put("message", "Erro de validação nos campos do formulário.");

        Map<String, List<String>> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        });
        errorBody.put("details", details);

        return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "BAD_REQUEST");
        errorBody.put("message", ex.getMessage());
        return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralExceptions(Exception ex) {
        log.error("Erro inesperado no servidor", ex);
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", "INTERNAL_SERVER_ERROR");
        errorBody.put("message", "Ocorreu um erro interno de processamento.");
        return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
