package com.camplog.pokedex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePublicProfileDto {
    private String username;
    private String bio;
    private String themeColors;
}
