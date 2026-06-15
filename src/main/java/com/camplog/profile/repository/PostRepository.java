package com.camplog.profile.repository;

import com.camplog.profile.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.createdAt < :cursor ORDER BY p.createdAt DESC")
    List<Post> findByAuthorIdWithCursor(
            @Param("authorId") String authorId,
            @Param("cursor") LocalDateTime cursor,
            Pageable pageable
    );

    int countByAuthorId(String authorId);
}
