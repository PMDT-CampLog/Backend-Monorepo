package com.camplog.connection.event;

import com.camplog.connection.model.ProfileType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Evento de domínio disparado quando um perfil deixa de seguir outro.
 *
 * Complementa o {@link ProfileFollowedEvent} — permite que módulos reativos
 * (ex: sistema de notificações, contagem de seguidores cacheada) atualizem
 * seu estado quando uma conexão é removida.
 */
@Getter
public class ProfileUnfollowedEvent extends ApplicationEvent {

    private final String followerId;
    private final ProfileType followerType;
    private final String followedId;
    private final ProfileType followedType;
    private final LocalDateTime occurredAt;

    public ProfileUnfollowedEvent(
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
