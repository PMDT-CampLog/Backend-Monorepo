package com.camplog.connection.controller;

import com.camplog.auth.model.User;
import com.camplog.connection.dto.ConnectionResponse;
import com.camplog.connection.dto.ConnectionStatusResponse;
import com.camplog.connection.dto.FollowRequest;
import com.camplog.connection.model.ProfileType;
import com.camplog.connection.service.ConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para o módulo de conexões (follow/unfollow).
 *
 * DECISÃO DE DESIGN DA API:
 * - POST /follow e DELETE /unfollow aceitam o alvo via body (FollowRequest).
 *   O follower é SEMPRE inferido do SecurityContext — nunca enviado pelo client.
 * - GET /followers e /following aceitam o perfil via path params (público).
 * - GET /status requer autenticação (verifica se o caller segue o alvo).
 *
 * RATE LIMITING:
 * Aplicado via {@link com.camplog.connection.config.RateLimitingConfig} como
 * filtro HTTP — 30 writes/min e 120 reads/min por IP. Os headers
 * X-RateLimit-Limit, X-RateLimit-Remaining e X-RateLimit-Reset são incluídos
 * em todas as respostas.
 */
@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
@Slf4j
public class ConnectionController {

    private final ConnectionService connectionService;

    /**
     * Seguir um perfil.
     *
     * POST /api/v1/connections/follow
     * Body: { "targetId": "uuid", "targetType": "CREATOR" | "SUPPORTER" }
     */
    @PostMapping("/follow")
    public ResponseEntity<Void> follow(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FollowRequest request
    ) {
        log.info("POST /connections/follow — User: {}, Target: {}({})",
                user.getId(), request.getTargetId(), request.getTargetType());

        connectionService.follow(user, request.getTargetId(), request.getTargetType());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Deixar de seguir um perfil.
     *
     * DELETE /api/v1/connections/unfollow
     * Body: { "targetId": "uuid", "targetType": "CREATOR" | "SUPPORTER" }
     */
    @DeleteMapping("/unfollow")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FollowRequest request
    ) {
        log.info("DELETE /connections/unfollow — User: {}, Target: {}({})",
                user.getId(), request.getTargetId(), request.getTargetType());

        connectionService.unfollow(user, request.getTargetId(), request.getTargetType());

        return ResponseEntity.noContent().build();
    }

    /**
     * Listar os seguidores de um perfil (público, paginado).
     *
     * GET /api/v1/connections/{profileId}/{profileType}/followers?page=0&size=20
     */
    @GetMapping("/{profileId}/{profileType}/followers")
    public ResponseEntity<Page<ConnectionResponse>> getFollowers(
            @PathVariable String profileId,
            @PathVariable ProfileType profileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /connections/{}/{}/followers — page={}, size={}",
                profileId, profileType, page, size);

        Page<ConnectionResponse> followers = connectionService.getFollowers(
                profileId, profileType,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return ResponseEntity.ok(followers);
    }

    /**
     * Listar quem um perfil está seguindo (público, paginado).
     *
     * GET /api/v1/connections/{profileId}/{profileType}/following?page=0&size=20
     */
    @GetMapping("/{profileId}/{profileType}/following")
    public ResponseEntity<Page<ConnectionResponse>> getFollowing(
            @PathVariable String profileId,
            @PathVariable ProfileType profileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /connections/{}/{}/following — page={}, size={}",
                profileId, profileType, page, size);

        Page<ConnectionResponse> following = connectionService.getFollowing(
                profileId, profileType,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return ResponseEntity.ok(following);
    }

    /**
     * Verificar o status de conexão entre o caller e um perfil alvo.
     * Requer autenticação — retorna isFollowing + contagens.
     *
     * GET /api/v1/connections/status?targetId=uuid&targetType=CREATOR
     */
    @GetMapping("/status")
    public ResponseEntity<ConnectionStatusResponse> getConnectionStatus(
            @AuthenticationPrincipal User user,
            @RequestParam String targetId,
            @RequestParam ProfileType targetType
    ) {
        log.info("GET /connections/status — User: {}, Target: {}({})",
                user.getId(), targetId, targetType);

        ConnectionStatusResponse status = connectionService.getConnectionStatus(
                user, targetId, targetType
        );

        return ResponseEntity.ok(status);
    }
}
