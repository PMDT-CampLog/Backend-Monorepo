package com.camplog.profile.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "O nome de exibição deve ter no máximo 100 caracteres")
    private String displayName;

    @Size(max = 500, message = "A bio deve ter no máximo 500 caracteres")
    private String bio;

    @Size(max = 2000, message = "A bio estendida deve ter no máximo 2000 caracteres")
    private String bioExtended;

    @Size(max = 500, message = "A URL do website deve ter no máximo 500 caracteres")
    private String websiteUrl;

    @Size(max = 150, message = "A localização deve ter no máximo 150 caracteres")
    private String location;
}
