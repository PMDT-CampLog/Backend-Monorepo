package com.camplog.connection.service;

import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import com.camplog.connection.dto.ConnectionResponse;
import com.camplog.connection.dto.ConnectionStatusResponse;
import com.camplog.connection.event.ProfileFollowedEvent;
import com.camplog.connection.event.ProfileUnfollowedEvent;
import com.camplog.connection.model.Connection;
import com.camplog.connection.model.ProfileType;
import com.camplog.connection.repository.ConnectionRepository;
import com.camplog.pokedex.model.PublicProfile;
import com.camplog.pokedex.repository.PublicProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;

/**
 * Serviço central do módulo de conexões (follow/unfollow).
 *
 * DECISÃO DE INJEÇÃO DE DEPENDÊNCIA:
 * - {@code ConnectionRepository}: dependência interna do próprio módulo.
 * - {@code UserRepository}: dependência cross-module (auth → connection).
 *   Utilizada SOMENTE em leituras para enriquecer respostas com displayName/avatarUrl.
 *   O módulo connection nunca altera dados do módulo auth.
 * - {@code ApplicationEventPublisher}: mecanismo do Spring para desacoplamento.
 *   Dispara eventos que qualquer módulo pode escutar sem criar dependência direta.
 *
 * ISOLAMENTO ARQUITETURAL:
 * - A lógica de negócio do follow/unfollow NÃO importa classes de profile
 *   (SupporterProfile, CreatorProfile). Usa apenas User para resolver
 *   informações de exibição. O tipo de perfil é um enum puro.
 * - Eventos de domínio garantem que os módulos de Notificações, Devlogs e
 *   Data Platform reajam sem acoplamento direto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PublicProfileRepository publicProfileRepository;

    /**
     * Resolve o ProfileType a partir do role do usuário autenticado.
     * O campo User.role pode conter valores como "creator", "apoiador", "member".
     * Mapeia "creator" → CREATOR e "apoiador"/"member" → SUPPORTER.
     */
    public ProfileType resolveProfileType(User user) {
        if ("creator".equalsIgnoreCase(user.getRole())) {
            return ProfileType.CREATOR;
        }
        return ProfileType.SUPPORTER;
    }

    /**
     * Cria uma conexão de follow entre o usuário autenticado e o perfil alvo.
     *
     * Validações:
     * 1. Não permite seguir a si mesmo.
     * 2. Não permite duplicar uma conexão existente.
     *
     * Após a persistência, dispara {@link ProfileFollowedEvent} para
     * que outros módulos reajam (ex: notificações, feed, analytics).
     */
    @Transactional
    public void follow(User authenticatedUser, String targetId, ProfileType targetType) {
        ProfileType currentUserType = resolveProfileType(authenticatedUser);
        String currentUserId = authenticatedUser.getId();

        log.info("Follow solicitado: {}({}) → {}({})",
                currentUserId, currentUserType, targetId, targetType);

        // Validação: não pode seguir a si mesmo
        if (currentUserId.equals(targetId) && currentUserType == targetType) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível seguir a si mesmo."
            );
        }

        // Validação: não duplicar conexão
        boolean alreadyFollowing = connectionRepository
                .existsByFollowerIdAndFollowerTypeAndFollowedIdAndFollowedType(
                        currentUserId, currentUserType, targetId, targetType
                );

        if (alreadyFollowing) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Você já está seguindo este perfil."
            );
        }

        // Persistência da conexão
        Connection connection = Connection.builder()
                .followerId(currentUserId)
                .followerType(currentUserType)
                .followedId(targetId)
                .followedType(targetType)
                .build();

        connectionRepository.save(connection);

        log.info("Conexão criada com sucesso: {} → {}", currentUserId, targetId);

        // Disparo do evento de domínio — desacoplamento via Spring Events
        eventPublisher.publishEvent(new ProfileFollowedEvent(
                this, currentUserId, currentUserType, targetId, targetType
        ));
    }

    /**
     * Remove uma conexão de follow (unfollow).
     *
     * Dispara {@link ProfileUnfollowedEvent} após a remoção para que
     * módulos como contagem de seguidores cacheada possam se atualizar.
     */
    @Transactional
    public void unfollow(User authenticatedUser, String targetId, ProfileType targetType) {
        ProfileType currentUserType = resolveProfileType(authenticatedUser);
        String currentUserId = authenticatedUser.getId();

        log.info("Unfollow solicitado: {}({}) → {}({})",
                currentUserId, currentUserType, targetId, targetType);

        long deleted = connectionRepository
                .deleteByFollowerIdAndFollowerTypeAndFollowedIdAndFollowedType(
                        currentUserId, currentUserType, targetId, targetType
                );

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Conexão não encontrada. Você não está seguindo este perfil."
            );
        }

        log.info("Conexão removida com sucesso: {} → {}", currentUserId, targetId);

        // Disparo do evento de domínio
        eventPublisher.publishEvent(new ProfileUnfollowedEvent(
                this, currentUserId, currentUserType, targetId, targetType
        ));
    }

    /**
     * Retorna a lista paginada de seguidores de um perfil.
     * Cada entrada é enriquecida com displayName e avatarUrl via UserRepository.
     */
    @Transactional
    public Page<ConnectionResponse> getFollowers(String profileId, ProfileType profileType, Pageable pageable) {
        log.info("Listando seguidores de {}({})", profileId, profileType);

        return connectionRepository
                .findByFollowedIdAndFollowedType(profileId, profileType, pageable)
                .map(connection -> enrichConnectionResponse(
                        connection.getFollowerId(),
                        connection.getFollowerType(),
                        connection.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME)
                ));
    }

    /**
     * Retorna a lista paginada de perfis que um perfil está seguindo.
     * Cada entrada é enriquecida com displayName e avatarUrl via UserRepository.
     */
    @Transactional
    public Page<ConnectionResponse> getFollowing(String profileId, ProfileType profileType, Pageable pageable) {
        log.info("Listando seguidos por {}({})", profileId, profileType);

        return connectionRepository
                .findByFollowerIdAndFollowerType(profileId, profileType, pageable)
                .map(connection -> enrichConnectionResponse(
                        connection.getFollowedId(),
                        connection.getFollowedType(),
                        connection.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME)
                ));
    }

    /**
     * Retorna o status de conexão entre o usuário autenticado e um perfil alvo.
     * Inclui se está seguindo + contagens de seguidores/seguindo do alvo.
     */
    @Transactional(readOnly = true)
    public ConnectionStatusResponse getConnectionStatus(
            User authenticatedUser,
            String targetId,
            ProfileType targetType
    ) {
        ProfileType currentUserType = resolveProfileType(authenticatedUser);
        String currentUserId = authenticatedUser.getId();

        boolean isFollowing = connectionRepository
                .existsByFollowerIdAndFollowerTypeAndFollowedIdAndFollowedType(
                        currentUserId, currentUserType, targetId, targetType
                );

        long followersCount = connectionRepository
                .countByFollowedIdAndFollowedType(targetId, targetType);

        long followingCount = connectionRepository
                .countByFollowerIdAndFollowerType(targetId, targetType);

        return ConnectionStatusResponse.builder()
                .isFollowing(isFollowing)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .build();
    }

    private ConnectionResponse enrichConnectionResponse(
            String profileId, ProfileType profileType, String connectedAt
    ) {
        // Busca dados de exibição do usuário pelo ID
        String displayName = "Usuário";
        String avatarUrl = null;
        String bio = null;
        String username = null;

        try {
            User user = userRepository.findById(profileId).orElse(null);
            if (user != null) {
                displayName = user.getName();
                avatarUrl = user.getAvatarUrl();
                bio = user.getBio();

                PublicProfile pub = publicProfileRepository.findByUserId(profileId).orElse(null);
                if (pub == null) {
                    // Auto-criar perfil público para auto-cura do banco de dados
                    String baseUsername = user.getName().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    if (baseUsername.isEmpty()) {
                        baseUsername = "user";
                    }
                    String finalUsername = baseUsername;
                    int suffix = 1;
                    while (publicProfileRepository.existsByUsername(finalUsername)) {
                        finalUsername = baseUsername + suffix;
                        suffix++;
                    }
                    pub = PublicProfile.builder()
                            .user(user)
                            .username(finalUsername)
                            .build();
                    publicProfileRepository.save(pub);
                }
                if (pub != null) {
                    username = pub.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível enriquecer dados do perfil {}: {}", profileId, e.getMessage());
        }

        return ConnectionResponse.builder()
                .profileId(profileId)
                .profileType(profileType)
                .displayName(displayName)
                .username(username)
                .avatarUrl(avatarUrl)
                .bio(bio)
                .connectedAt(connectedAt)
                .build();
    }
}
