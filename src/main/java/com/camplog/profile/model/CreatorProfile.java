package com.camplog.profile.model;

import com.camplog.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade JPA para perfis de criadores de conteúdo.
 *
 * DECISÃO ARQUITETURAL:
 * Reside no módulo profile (ao invés de um módulo creator separado) pois
 * compartilha a mesma infraestrutura de repositórios JPA, DTOs e serviços
 * com o SupporterProfile. Ambos os perfis derivam de um User, mas possuem
 * campos específicos ao seu domínio.
 *
 * Campos específicos do criador:
 * - category: área de atuação (ex: "gamedev", "webdev", "design")
 * - projectsCount: cache desnormalizado da contagem de projetos
 *
 * Espelha a estrutura do {@link SupporterProfile} para consistência.
 */
@Entity
@Table(name = "creator_profiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "bio_extended", columnDefinition = "TEXT")
    private String bioExtended;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(length = 150)
    private String location;

    /**
     * Área de atuação do criador (ex: "gamedev", "webdev", "design", "music").
     * Utilizado como facet de busca no motor de pesquisa.
     */
    @Column(length = 100)
    private String category;

    /**
     * Cache desnormalizado da contagem de projetos do criador.
     * Atualizado via eventos do módulo de projetos para evitar N+1 queries.
     */
    @Column(name = "projects_count", nullable = false)
    @Builder.Default
    private int projectsCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
