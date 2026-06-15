package com.camplog.profile.service;

import com.camplog.auth.model.User;
import com.camplog.profile.dto.CreatePostRequest;
import com.camplog.profile.dto.PostPageResponse;
import com.camplog.profile.dto.PostResponse;
import com.camplog.profile.model.Post;
import com.camplog.profile.model.PostMedia;
import com.camplog.profile.model.PostType;
import com.camplog.profile.repository.PostLikeRepository;
import com.camplog.profile.repository.PostMediaRepository;
import com.camplog.profile.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostLikeRepository postLikeRepository;
    private final MediaService mediaService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Transactional
    public PostResponse createPost(User author, CreatePostRequest request) {
        log.info("Criando post do tipo {} para o usuário: {}", request.getType(), author.getId());

        PostType type;
        try {
            type = PostType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de post inválido. Use TEXT ou IMAGE.");
        }

        Post post = Post.builder()
                .author(author)
                .type(type)
                .content(request.getContent())
                .latexEnabled(request.getLatexEnabled() != null ? request.getLatexEnabled() : false)
                .build();

        Post savedPost = postRepository.save(post);
        log.info("Post criado com ID: {}", savedPost.getId());

        return toPostResponse(savedPost, author.getId());
    }

    @Transactional(readOnly = true)
    public PostPageResponse getPostsByUser(String authorId, String cursor, Integer size, String requesterId) {
        int pageSize = (size != null && size > 0 && size <= 50) ? size : DEFAULT_PAGE_SIZE;

        List<Post> posts;
        if (cursor != null && !cursor.isEmpty()) {
            LocalDateTime cursorDate = LocalDateTime.parse(cursor, DateTimeFormatter.ISO_DATE_TIME);
            posts = postRepository.findByAuthorIdWithCursor(authorId, cursorDate, PageRequest.of(0, pageSize + 1));
        } else {
            var page = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(0, pageSize + 1));
            posts = page.getContent();
        }

        boolean hasNextPage = posts.size() > pageSize;
        if (hasNextPage) {
            posts = posts.subList(0, pageSize);
        }

        String nextCursor = hasNextPage && !posts.isEmpty()
                ? posts.get(posts.size() - 1).getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME)
                : null;

        List<PostResponse> postResponses = posts.stream()
                .map(post -> toPostResponse(post, requesterId))
                .collect(Collectors.toList());

        int total = postRepository.countByAuthorId(authorId);

        return PostPageResponse.builder()
                .data(postResponses)
                .total(total)
                .nextCursor(nextCursor)
                .hasNextPage(hasNextPage)
                .build();
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(String postId, String requesterId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado."));
        return toPostResponse(post, requesterId);
    }

    @Transactional
    public PostResponse updatePost(User author, String postId, CreatePostRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado."));

        if (!post.getAuthor().getId().equals(author.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para editar este post.");
        }

        post.setContent(request.getContent());
        if (request.getLatexEnabled() != null) {
            post.setLatexEnabled(request.getLatexEnabled());
        }

        Post savedPost = postRepository.save(post);
        return toPostResponse(savedPost, author.getId());
    }

    @Transactional
    public void deletePost(User author, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado."));

        if (!post.getAuthor().getId().equals(author.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para deletar este post.");
        }

        // Deleta mídias do storage
        for (PostMedia media : post.getMedia()) {
            mediaService.deleteMedia(media.getMediaKey());
        }

        postRepository.delete(post);
        log.info("Post {} e suas mídias deletados com sucesso", postId);
    }

    @Transactional
    public PostResponse addMediaToPost(User author, String postId, MultipartFile file) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado."));

        if (!post.getAuthor().getId().equals(author.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem permissão para adicionar mídia a este post.");
        }

        MediaService.MediaUploadResult result = mediaService.uploadPostImage(file, author.getId(), postId);

        int nextPosition = post.getMedia().size();
        PostMedia media = PostMedia.builder()
                .post(post)
                .mediaUrl(result.url())
                .mediaKey(result.key())
                .position(nextPosition)
                .build();

        postMediaRepository.save(media);

        // Atualiza o tipo para IMAGE se ainda não for
        if (post.getType() == PostType.TEXT) {
            post.setType(PostType.IMAGE);
            postRepository.save(post);
        }

        return toPostResponse(postRepository.findById(postId).orElseThrow(), author.getId());
    }

    @Transactional
    public boolean toggleLike(User user, String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post não encontrado."));

        var existingLike = postLikeRepository.findByUserIdAndPostId(user.getId(), postId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            postRepository.save(post);
            log.info("Usuário {} descurtiu o post {}", user.getId(), postId);
            return false; // descurtiu
        } else {
            var like = com.camplog.profile.model.PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            postLikeRepository.save(like);
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);
            log.info("Usuário {} curtiu o post {}", user.getId(), postId);
            return true; // curtiu
        }
    }

    @Transactional(readOnly = true)
    public PostPageResponse getLikedPosts(String userId, int page, int size, String requesterId) {
        var likedPostIds = postLikeRepository.findPostIdsByUserId(userId, PageRequest.of(page, size));

        List<PostResponse> posts = likedPostIds.getContent().stream()
                .map(postId -> postRepository.findById(postId).orElse(null))
                .filter(p -> p != null)
                .map(post -> toPostResponse(post, requesterId))
                .collect(Collectors.toList());

        return PostPageResponse.builder()
                .data(posts)
                .total((int) likedPostIds.getTotalElements())
                .hasNextPage(likedPostIds.hasNext())
                .nextCursor(null) // Paginação offset-based para curtidas
                .build();
    }

    private PostResponse toPostResponse(Post post, String requesterId) {
        boolean likedByMe = requesterId != null && postLikeRepository.existsByUserIdAndPostId(requesterId, post.getId());

        List<PostResponse.MediaResponse> mediaResponses = post.getMedia().stream()
                .map(m -> PostResponse.MediaResponse.builder()
                        .id(m.getId())
                        .mediaUrl(m.getMediaUrl())
                        .position(m.getPosition())
                        .build())
                .collect(Collectors.toList());

        User author = post.getAuthor();

        return PostResponse.builder()
                .id(post.getId())
                .authorId(author.getId())
                .authorName(author.getName())
                .authorAvatarUrl(author.getAvatarUrl())
                .type(post.getType().name())
                .content(post.getContent())
                .latexEnabled(post.getLatexEnabled())
                .media(mediaResponses)
                .likesCount(post.getLikesCount())
                .likedByMe(likedByMe)
                .createdAt(post.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .updatedAt(post.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }
}
