package com.camplog.profile.repository;

import com.camplog.profile.model.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {
    Optional<PostLike> findByUserIdAndPostId(String userId, String postId);
    boolean existsByUserIdAndPostId(String userId, String postId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId")
    Page<String> findPostIdsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT SUM(p.likesCount) FROM Post p WHERE p.author.id = :authorId")
    Integer countTotalLikesReceivedByAuthor(@Param("authorId") String authorId);
}
