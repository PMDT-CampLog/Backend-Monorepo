package com.camplog.profile.model;

import com.camplog.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_likes_user_post", columnNames = {"user_id", "post_id"})
}, indexes = {
        @Index(name = "idx_post_likes_user_post", columnList = "user_id, post_id"),
        @Index(name = "idx_post_likes_user_created", columnList = "user_id, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
