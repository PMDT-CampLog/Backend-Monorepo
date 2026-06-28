package com.camplog.connection.repository;

import com.camplog.connection.model.Connection;
import com.camplog.connection.model.ProfileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository de acesso a dados para a tabela de conexões.
 *
 * DECISÃO DE DESIGN:
 * Todas as queries derivadas utilizam os 4 campos do polimorfismo (id + type)
 * para garantir que uma busca por seguidores de um CREATOR não retorne
 * acidentalmente seguidores de um SUPPORTER com o mesmo ID (cenário improvável
 * com UUIDs, mas semanticamente correto).
 *
 * As queries de listagem retornam Page<Connection> para suportar paginação
 * eficiente no controller via Pageable do Spring Data.
 */
@Repository
public interface ConnectionRepository extends JpaRepository<Connection, String> {

    /**
     * Verifica se uma conexão específica já existe.
     * Usado para prevenir duplicatas e para o endpoint de status (isFollowing).
     */
    boolean existsByFollowerIdAndFollowerTypeAndFollowedIdAndFollowedType(
            String followerId, ProfileType followerType,
            String followedId, ProfileType followedType
    );

    /**
     * Remove uma conexão específica (unfollow).
     * Retorna o número de linhas deletadas (0 ou 1).
     */
    long deleteByFollowerIdAndFollowerTypeAndFollowedIdAndFollowedType(
            String followerId, ProfileType followerType,
            String followedId, ProfileType followedType
    );

    /**
     * Busca todos os seguidores de um perfil específico (paginado).
     * Utilizado no endpoint GET /connections/{id}/{type}/followers.
     */
    Page<Connection> findByFollowedIdAndFollowedType(
            String followedId, ProfileType followedType, Pageable pageable
    );

    /**
     * Busca todos os perfis que um perfil específico está seguindo (paginado).
     * Utilizado no endpoint GET /connections/{id}/{type}/following.
     */
    Page<Connection> findByFollowerIdAndFollowerType(
            String followerId, ProfileType followerType, Pageable pageable
    );

    /**
     * Conta o total de seguidores de um perfil.
     */
    long countByFollowedIdAndFollowedType(String followedId, ProfileType followedType);

    /**
     * Conta o total de perfis que um perfil está seguindo.
     */
    long countByFollowerIdAndFollowerType(String followerId, ProfileType followerType);
}
