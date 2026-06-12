package com.camplog.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "O e-mail inserido é inválido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    private String password;
}
