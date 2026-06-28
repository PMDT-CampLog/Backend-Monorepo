package com.camplog.connection.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuração de Rate Limiting para o módulo de conexões.
 *
 * DECISÃO DE DESIGN:
 * Implementação in-memory com Sliding Window Counter usando ConcurrentHashMap.
 * Adequado para o cenário atual (monólito single-instance). Para múltiplas instâncias,
 * migrar para Redis (ex: Bucket4j + Redis ou Spring Cloud Gateway Rate Limiter).
 *
 * Limites:
 * - POST/DELETE (follow/unfollow): 30 requests por minuto por IP
 * - GET (listagens/status): 120 requests por minuto por IP
 *
 * Escopo do filtro: apenas URLs que começam com /api/v1/connections/
 */
@Configuration
@Slf4j
public class RateLimitingConfig {

    /** Janela de tempo em milissegundos (1 minuto). */
    private static final long WINDOW_MS = 60_000L;

    /** Limite para operações de escrita (follow/unfollow). */
    private static final int WRITE_LIMIT = 30;

    /** Limite para operações de leitura (listagens, status). */
    private static final int READ_LIMIT = 120;

    /**
     * Armazena o estado de rate limiting por chave (IP + tipo de operação).
     * Cada entrada contém: [contagem, timestamp do início da janela].
     */
    private final Map<String, long[]> rateLimitState = new ConcurrentHashMap<>();

    @Bean
    public FilterRegistrationBean<Filter> connectionRateLimitFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new Filter() {
            @Override
            public void doFilter(
                    ServletRequest servletRequest,
                    ServletResponse servletResponse,
                    FilterChain chain
            ) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;

                String path = request.getRequestURI();

                // Aplicar rate limiting apenas às rotas de conexões
                if (!path.startsWith("/api/v1/connections")) {
                    chain.doFilter(request, response);
                    return;
                }

                String method = request.getMethod();
                String clientIp = extractClientIp(request);

                boolean isWriteOperation = "POST".equalsIgnoreCase(method)
                        || "DELETE".equalsIgnoreCase(method);

                int limit = isWriteOperation ? WRITE_LIMIT : READ_LIMIT;
                String rateLimitKey = clientIp + ":" + (isWriteOperation ? "write" : "read");

                if (!isAllowed(rateLimitKey, limit)) {
                    log.warn("[RATE LIMIT] Limite excedido para {}: {} {} (limite: {}/min)",
                            clientIp, method, path, limit);

                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write(String.format(
                            "{\"code\":\"RATE_LIMIT_EXCEEDED\","
                            + "\"message\":\"Limite de requisições excedido. Tente novamente em breve.\","
                            + "\"retryAfterSeconds\":60,"
                            + "\"limit\":%d}",
                            limit
                    ));
                    return;
                }

                // Adiciona headers informativos de rate limiting na resposta
                long[] state = rateLimitState.get(rateLimitKey);
                int remaining = state != null ? Math.max(0, limit - (int) state[0]) : limit;

                response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                response.setHeader("X-RateLimit-Reset",
                        String.valueOf((System.currentTimeMillis() / 1000) + 60));

                chain.doFilter(request, response);
            }
        });

        // Aplicar o filtro apenas às rotas de conexões
        registration.addUrlPatterns("/api/v1/connections/*");
        registration.setOrder(1);
        registration.setName("connectionRateLimitFilter");

        return registration;
    }

    /**
     * Verifica se a requisição é permitida dentro da janela de rate limiting.
     * Implementa Sliding Window Counter com reset automático ao expirar a janela.
     *
     * @param key     Chave única (IP + tipo de operação)
     * @param limit   Número máximo de requisições permitidas na janela
     * @return true se a requisição é permitida
     */
    private synchronized boolean isAllowed(String key, int limit) {
        long now = System.currentTimeMillis();
        long[] state = rateLimitState.get(key);

        if (state == null || (now - state[1]) > WINDOW_MS) {
            // Nova janela ou janela expirada — reset
            rateLimitState.put(key, new long[]{1, now});
            return true;
        }

        if (state[0] < limit) {
            state[0]++;
            return true;
        }

        return false;
    }

    /**
     * Extrai o IP real do cliente, considerando proxies reversos (X-Forwarded-For).
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Pega o primeiro IP da cadeia (IP do cliente original)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
