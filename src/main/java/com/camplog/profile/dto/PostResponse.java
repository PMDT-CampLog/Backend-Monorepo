package com.camplog.profile.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private String id;
    private String authorId;
    private String authorName;
    private String authorAvatarUrl;
    private String type;
    private String content;
    private boolean latexEnabled;
    private List<MediaResponse> media;
    private int likesCount;
    private boolean likedByMe;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaResponse {
        private String id;
        private String mediaUrl;
        private int position;
    }
}
