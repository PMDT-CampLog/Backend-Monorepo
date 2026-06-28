package com.camplog.connection.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade JPA que modela a tabela polimórfica de conexões (follow/unfollow).
 *
 * DECISÃO DE MODELAGEM:
 * - Não utilizamos @ManyToOne para referenciar as entidades de perfil diretamente.
 *   Isso é intencional: o módulo de conexões NÃO deve depender dos módulos de perfil
 *   (CreatorProfile, SupporterProfile). Os IDs são armazenados como Strings simples
 *   e o tipo é resolvido pelo enum {@link ProfileType}.
 *
 * - O polimorfismo é alcançado via colunas discriminadoras (follower_type, followed_type)
 *   ao invés de herança JPA, o que é mais flexível e performático para este caso de uso.
 *
 * ÍNDICES:
 * - idx_connection_followed: otimiza "buscar todos os seguidores de X"
 * - idx_connection_follower: otimiza "buscar quem X está seguindo"
 * - uk_connection_unique: garante que um perfil não pode seguir o mesmo perfil duas vezes
 */
@Entity
@Table(
    name = "connections",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_connection_unique",
            columnNames = {"follower_id", "follower_type", "followed_id", "followed_type"}
        )
    },
    indexes = {
        @Index(name = "idx_connection_followed", columnList = "followed_id, followed_type"),
        @Index(name = "idx_connection_follower", columnList = "follower_id, follower_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * ID do perfil que está realizando a ação de seguir.
     * Referência lógica (não FK) — mantém o isolamento modular.
     */
    @Column(name = "follower_id", nullable = false)
    private String followerId;

    /**
     * Tipo do perfil seguidor — discriminador polimórfico.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "follower_type", nullable = false, length = 20)
    private ProfileType followerType;

    /**
     * ID do perfil que está sendo seguido.
     * Referência lógica (não FK) — mantém o isolamento modular.
     */
    @Column(name = "followed_id", nullable = false)
    private String followedId;

    /**
     * Tipo do perfil seguido — discriminador polimórfico.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "followed_type", nullable = false, length = 20)
    private ProfileType followedType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
