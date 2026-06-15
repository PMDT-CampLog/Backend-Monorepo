package com.camplog.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "O conteúdo do post é obrigatório")
    private String content;

    @NotNull(message = "O tipo do post é obrigatório (TEXT ou IMAGE)")
    private String type;

    @Builder.Default
    private Boolean latexEnabled = false;
}
