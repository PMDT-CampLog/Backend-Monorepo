package com.camplog.profile.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private String userId;
    private String name;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String coverUrl;
    private String bio;
    private String bioExtended;
    private String websiteUrl;
    private String location;
    private String role;
    private int postsCount;
    private int likesReceivedCount;
    private List<String> interests;
    private String createdAt;
}
