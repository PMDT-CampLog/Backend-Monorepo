package com.camplog.connection.dto;

import com.camplog.connection.model.ProfileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para as operações de follow e unfollow.
 *
 * O caller informa apenas o alvo (targetId + targetType).
 * O follower é inferido a partir do usuário autenticado (SecurityContext),
 * garantindo que ninguém pode forjar um follow em nome de outro usuário.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequest {

    @NotBlank(message = "O ID do perfil alvo é obrigatório.")
    private String targetId;

    @NotNull(message = "O tipo do perfil alvo é obrigatório.")
    private ProfileType targetType;
}
