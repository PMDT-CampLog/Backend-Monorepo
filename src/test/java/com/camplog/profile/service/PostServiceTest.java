package com.camplog.profile.service;

import com.camplog.auth.model.User;
import com.camplog.profile.dto.CreatePostRequest;
import com.camplog.profile.dto.PostPageResponse;
import com.camplog.profile.dto.PostResponse;
import com.camplog.profile.model.Post;
import com.camplog.profile.model.PostMedia;
import com.camplog.profile.model.PostType;
import com.camplog.profile.model.PostLike;
import com.camplog.profile.repository.PostLikeRepository;
import com.camplog.profile.repository.PostMediaRepository;
import com.camplog.profile.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMediaRepository postMediaRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private PostService postService;

    private User author;
    private User otherUser;
    private Post post;
    private CreatePostRequest request;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id("user-123")
                .name("Test User")
                .email("test@camplog.com")
                .role("apoiador")
                .build();

        otherUser = User.builder()
                .id("user-456")
                .name("Other User")
                .email("other@camplog.com")
                .role("member")
                .build();

        post = Post.builder()
                .id("post-123")
                .author(author)
                .type(PostType.TEXT)
                .content("Original content")
                .latexEnabled(false)
                .likesCount(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .media(new ArrayList<>())
                .build();

        request = CreatePostRequest.builder()
                .content("New post content")
                .type("TEXT")
                .latexEnabled(true)
                .build();
    }

    @Test
    void createPost_withValidRequest_returnsPostResponse() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId("new-post-id");
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            p.setMedia(new ArrayList<>());
            return p;
        });

        PostResponse response = postService.createPost(author, request);

        assertNotNull(response);
        assertEquals("new-post-id", response.getId());
        assertEquals("New post content", response.getContent());
        assertEquals("TEXT", response.getType());
        assertTrue(response.isLatexEnabled());
        assertEquals(author.getId(), response.getAuthorId());
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    void createPost_withInvalidType_throwsBadRequest() {
        request.setType("INVALID_TYPE");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            postService.createPost(author, request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void getPostById_existingPost_returnsPostResponse() {
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));
        when(postLikeRepository.existsByUserIdAndPostId("user-123", "post-123")).thenReturn(true);

        PostResponse response = postService.getPostById("post-123", "user-123");

        assertNotNull(response);
        assertEquals("post-123", response.getId());
        assertTrue(response.isLikedByMe());
        assertEquals(5, response.getLikesCount());
    }

    @Test
    void getPostById_nonExistentPost_throwsNotFound() {
        when(postRepository.findById("non-existent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            postService.getPostById("non-existent", "user-123");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updatePost_byOwner_updatesContentAndReturnsResponse() {
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        PostResponse response = postService.updatePost(author, "post-123", request);

        assertNotNull(response);
        assertEquals("New post content", response.getContent());
        assertTrue(response.isLatexEnabled());
        verify(postRepository, times(1)).save(post);
    }

    @Test
    void updatePost_byNonOwner_throwsForbidden() {
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            postService.updatePost(otherUser, "post-123", request);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_byOwner_deletesPostAndMedias() {
        PostMedia media = PostMedia.builder()
                .id("media-1")
                .mediaKey("s3-key-1")
                .mediaUrl("http://cdn/s3-key-1")
                .build();
        post.getMedia().add(media);

        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));
        doNothing().when(mediaService).deleteMedia("s3-key-1");
        doNothing().when(postRepository).delete(post);

        assertDoesNotThrow(() -> postService.deletePost(author, "post-123"));

        verify(mediaService, times(1)).deleteMedia("s3-key-1");
        verify(postRepository, times(1)).delete(post);
    }

    @Test
    void deletePost_byNonOwner_throwsForbidden() {
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            postService.deletePost(otherUser, "post-123");
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    void toggleLike_firstTime_createsLikeAndIncrementsCount() {
        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserIdAndPostId(author.getId(), "post-123")).thenReturn(Optional.empty());
        when(postLikeRepository.save(any(PostLike.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        boolean liked = postService.toggleLike(author, "post-123");

        assertTrue(liked);
        assertEquals(6, post.getLikesCount());
        verify(postLikeRepository, times(1)).save(any(PostLike.class));
        verify(postRepository, times(1)).save(post);
    }

    @Test
    void toggleLike_secondTime_removesLikeAndDecrementsCount() {
        PostLike like = PostLike.builder().id("like-1").user(author).post(post).build();

        when(postRepository.findById("post-123")).thenReturn(Optional.of(post));
        when(postLikeRepository.findByUserIdAndPostId(author.getId(), "post-123")).thenReturn(Optional.of(like));
        doNothing().when(postLikeRepository).delete(like);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        boolean liked = postService.toggleLike(author, "post-123");

        assertFalse(liked);
        assertEquals(4, post.getLikesCount());
        verify(postLikeRepository, times(1)).delete(like);
        verify(postRepository, times(1)).save(post);
    }
}
