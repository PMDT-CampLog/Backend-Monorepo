package com.camplog.profile.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInterestsRequest {

    @NotEmpty(message = "A lista de interesses não pode estar vazia")
    @Size(max = 20, message = "O limite máximo é de 20 interesses")
    private List<@Size(max = 100, message = "Cada interesse deve ter no máximo 100 caracteres") String> tags;
}
