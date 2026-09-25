package com.camplog.bond.service;

import com.camplog.bond.dto.ProfileResponse;
import com.camplog.bond.model.UserProfile;
import com.camplog.bond.repository.UserProfileRepository;
import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;


    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        return ProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .displayName(profile != null ? profile.getDisplayName() : user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .bio(user.getBio())
                .bioExtended(profile != null ? profile.getBioExtended() : null)
                .websiteUrl(profile != null ? profile.getWebsiteUrl() : null)
                .location(profile != null ? profile.getLocation() : null)
                .role(user.getRole())
                .createdAt(user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }


    public ProfileResponse updateAvatar(User authenticatedUser, MultipartFile file){
        log.info("Update de avatar para o usuário: {}", authenticatedUser.getId());

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        String avatarUrl = user.getAvatarUrl();

        MediaService.MediaUploadResult result = mediaService.uploadAvatar(file, user.getId());
        user.setAvatarUrl(result.url());
        userRepository.save(user);

        if(avatarUrl != null && !avatarUrl.isBlank()){
            String oldKey = mediaService.extractKeyFromUrl(avatarUrl);
            if(oldKey != null){
                mediaService.deleteMedia(oldKey);
            }
        }

        return getProfile(user.getId());
    }

    public ProfileResponse uploadCover(User authenticatedUser, MultipartFile file) {
        log.info("Upload de capa para o usuário: {}", authenticatedUser.getId());

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        String coverUrl = user.getCoverUrl();

        MediaService.MediaUploadResult result = mediaService.uploadCover(file, user.getId());
        user.setCoverUrl(result.url());
        userRepository.save(user);

        if(coverUrl != null && !coverUrl.isBlank()){
            String oldKey = mediaService.extractKeyFromUrl(coverUrl);
            if(oldKey != null){
                mediaService.deleteMedia(oldKey);
            }
        }

        return getProfile(user.getId());
    }
}
