package com.camplog.profile.controller;

import com.camplog.auth.model.User;
import com.camplog.profile.dto.CreatePostRequest;
import com.camplog.profile.dto.PostPageResponse;
import com.camplog.profile.dto.PostResponse;
import com.camplog.profile.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;

    @PostMapping("/me/posts")
    @PreAuthorize("hasRole('APOIADOR')")
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePostRequest request
    ) {
        log.info("Criação de post pelo apoiador: {}", user.getId());
        PostResponse post = postService.createPost(user, request);
        return new ResponseEntity<>(post, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<PostPageResponse> getUserPosts(
            @PathVariable String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @AuthenticationPrincipal User user
    ) {
        String requesterId = user != null ? user.getId() : null;
        PostPageResponse response = postService.getPostsByUser(userId, cursor, size, requesterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable String postId,
            @AuthenticationPrincipal User user
    ) {
        String requesterId = user != null ? user.getId() : null;
        PostResponse post = postService.getPostById(postId, requesterId);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/me/posts/{postId}")
    @PreAuthorize("hasRole('APOIADOR')")
    public ResponseEntity<PostResponse> updatePost(
            @AuthenticationPrincipal User user,
            @PathVariable String postId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostResponse post = postService.updatePost(user, postId, request);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/me/posts/{postId}")
    @PreAuthorize("hasRole('APOIADOR')")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal User user,
            @PathVariable String postId
    ) {
        postService.deletePost(user, postId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/posts/{postId}/media")
    @PreAuthorize("hasRole('APOIADOR')")
    public ResponseEntity<PostResponse> addMedia(
            @AuthenticationPrincipal User user,
            @PathVariable String postId,
            @RequestParam("file") MultipartFile file
    ) {
        PostResponse post = postService.addMediaToPost(user, postId, file);
        return ResponseEntity.ok(post);
    }

    // --- Like endpoints ---

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal User user,
            @PathVariable String postId
    ) {
        boolean liked = postService.toggleLike(user, postId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @GetMapping("/{userId}/likes")
    public ResponseEntity<PostPageResponse> getLikedPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        String requesterId = user != null ? user.getId() : null;
        PostPageResponse response = postService.getLikedPosts(userId, page, size, requesterId);
        return ResponseEntity.ok(response);
    }
}
