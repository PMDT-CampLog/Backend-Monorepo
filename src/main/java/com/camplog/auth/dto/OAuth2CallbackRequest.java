package com.camplog.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2CallbackRequest {
    @NotBlank(message = "O código do provedor OAuth é obrigatório")
    private String code;
}
