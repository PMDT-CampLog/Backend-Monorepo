package com.camplog.pokedex.service;

import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import com.camplog.pokedex.dto.PublicProfileDto;
import com.camplog.pokedex.dto.UpdatePublicProfileDto;
import com.camplog.pokedex.model.PublicProfile;
import com.camplog.pokedex.repository.PublicProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokedexService {

    private final PublicProfileRepository publicProfileRepository;
    private final UserRepository userRepository;
    private final DataPlatformClient dataPlatformClient;

    public PublicProfileDto getProfileByUsername(String username) {
        PublicProfile profile = publicProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado para o username: " + username));
        return mapToDto(profile);
    }

    public PublicProfileDto getProfileByUserId(String userId) {
        PublicProfile profile = publicProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado"));
        return mapToDto(profile);
    }

    public boolean isUsernameAvailable(String username) {
        return !publicProfileRepository.existsByUsername(username);
    }

    @Transactional
    public void createOrUpdateProfileSync(String userId, UpdatePublicProfileDto dto) {
        // Just text fields, no media
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PublicProfile profile = publicProfileRepository.findByUserId(userId)
                .orElse(PublicProfile.builder().user(user).build());

        if (dto.getUsername() != null && !dto.getUsername().equals(profile.getUsername())) {
            if (!isUsernameAvailable(dto.getUsername())) {
                log.warn("[PIKA] Username inválido recusado: " + dto.getUsername());
                throw new RuntimeException("Username já em uso");
            }
            profile.setUsername(dto.getUsername());
        }

        if (dto.getBio() != null) profile.setBio(dto.getBio());
        if (dto.getThemeColors() != null) profile.setThemeColors(dto.getThemeColors());

        publicProfileRepository.save(profile);
        log.info("[BULBA] Perfil público atualizado com sucesso (dados de texto) para userId: " + userId);
    }

    @Async
    @Transactional
    public void processMediaUploadAsync(String userId, MultipartFile avatarFile, MultipartFile coverFile) {
        try {
            PublicProfile profile = publicProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Perfil não encontrado antes do upload"));

            int retries = 3;
            boolean success = false;
            
            while (retries > 0 && !success) {
                try {
                    if (avatarFile != null && !avatarFile.isEmpty()) {
                        String avatarUrl = dataPlatformClient.uploadMedia(avatarFile, "avatars");
                        profile.setAvatarUrl(avatarUrl);
                    }
                    if (coverFile != null && !coverFile.isEmpty()) {
                        String coverUrl = dataPlatformClient.uploadMedia(coverFile, "covers");
                        profile.setCoverUrl(coverUrl);
                    }
                    success = true;
                } catch (Exception e) {
                    retries--;
                    if (retries == 0) {
                        log.error("[CHARMANDER] Erro crítico ao fazer upload para o Data Platform após várias tentativas: " + e.getMessage(), e);
                        throw e;
                    }
                    log.warn("[PIKA] Falha ao enviar mídia, tentando novamente...");
                    Thread.sleep(1000);
                }
            }

            publicProfileRepository.save(profile);
            
            if (retries == 3) {
                log.info("[BULBA] Operação de integração concluída de primeira e com sucesso para uploads do userId: " + userId);
            } else {
                log.info("[SQUIRTLE] Operação concluída após tentativa de retry por instabilidade momentânea para userId: " + userId);
            }
            
        } catch (Exception e) {
            log.error("[CHARMANDER] Erro crítico na thread assíncrona de mídia: " + e.getMessage(), e);
        }
    }

    private PublicProfileDto mapToDto(PublicProfile profile) {
        return PublicProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .username(profile.getUsername())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .coverUrl(profile.getCoverUrl())
                .themeColors(profile.getThemeColors())
                .build();
    }
}
