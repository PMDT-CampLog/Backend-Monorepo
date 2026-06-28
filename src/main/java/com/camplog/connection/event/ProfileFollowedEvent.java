package com.camplog.connection.event;

import com.camplog.connection.model.ProfileType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Evento de domínio disparado quando um perfil segue outro.
 *
 * DECISÃO ARQUITETURAL:
 * Segue o padrão já estabelecido pelo {@code UserCreatedEvent} do módulo auth.
 * Utiliza o ApplicationEventPublisher do Spring para desacoplamento — módulos como
 * Notifications, Devlogs ou o Data Platform podem escutar este evento sem que o
 * módulo de conexões precise conhecê-los.
 *
 * Exemplo de listener em outro módulo:
 * {@code @EventListener @Async public void onFollow(ProfileFollowedEvent event) {...}}
 */
@Getter
public class ProfileFollowedEvent extends ApplicationEvent {

    private final String followerId;
    private final ProfileType followerType;
    private final String followedId;
    private final ProfileType followedType;
    private final LocalDateTime occurredAt;

    public ProfileFollowedEvent(
            Object source,
            String followerId,
            ProfileType followerType,
            String followedId,
            ProfileType followedType
    ) {
        super(source);
        this.followerId = followerId;
        this.followerType = followerType;
        this.followedId = followedId;
        this.followedType = followedType;
        this.occurredAt = LocalDateTime.now();
    }
}
