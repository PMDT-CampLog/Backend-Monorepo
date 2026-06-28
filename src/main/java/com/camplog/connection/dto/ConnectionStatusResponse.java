package com.camplog.connection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta para o endpoint de status de conexão.
 * Utilizado pelo frontend para renderizar o estado do FollowButton
 * sem precisar carregar a lista completa de seguidores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionStatusResponse {

    /** Indica se o usuário autenticado está seguindo o perfil alvo. */
    private boolean isFollowing;

    /** Total de seguidores do perfil alvo. */
    private long followersCount;

    /** Total de perfis que o perfil alvo está seguindo. */
    private long followingCount;
}
