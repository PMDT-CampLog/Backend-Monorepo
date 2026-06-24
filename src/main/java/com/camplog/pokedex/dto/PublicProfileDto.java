package com.camplog.pokedex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDto {
    private String id;
    private String userId;
    private String username;
    private String bio;
    private String avatarUrl;
    private String coverUrl;
    private String themeColors;
}
