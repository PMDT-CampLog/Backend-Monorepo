package com.camplog.connection.dto;

import com.camplog.connection.model.ProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de saída para representar uma conexão na lista de seguidores/seguindo.
 *
 * NOTA: displayName e avatarUrl são campos desnormalizados que serão preenchidos
 * pelo ConnectionService através de uma consulta ao módulo profile. Isso é um
 * trade-off consciente: aceita-se o acoplamento de leitura (read-side) para
 * entregar uma API mais rica ao frontend, enquanto o acoplamento de escrita
 * (write-side) permanece zero entre os módulos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponse {

    private String profileId;
    private ProfileType profileType;
    private String displayName;
    private String username;
    private String avatarUrl;
    private String bio;
    private String connectedAt;
}
