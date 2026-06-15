package com.camplog.profile.service;

import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import com.camplog.profile.dto.ProfileResponse;
import com.camplog.profile.dto.UpdateProfileRequest;
import com.camplog.profile.model.SupporterProfile;
import com.camplog.profile.model.UserInterest;
import com.camplog.profile.repository.PostLikeRepository;
import com.camplog.profile.repository.PostRepository;
import com.camplog.profile.repository.SupporterProfileRepository;
import com.camplog.profile.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final SupporterProfileRepository supporterProfileRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserInterestRepository userInterestRepository;
    private final MediaService mediaService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        SupporterProfile profile = supporterProfileRepository.findByUserId(userId).orElse(null);

        int postsCount = postRepository.countByAuthorId(userId);
        Integer likesReceived = postLikeRepository.countTotalLikesReceivedByAuthor(userId);
        List<String> interests = userInterestRepository.findByUserId(userId).stream()
                .map(UserInterest::getTag)
                .collect(Collectors.toList());

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
                .postsCount(postsCount)
                .likesReceivedCount(likesReceived != null ? likesReceived : 0)
                .interests(interests)
                .createdAt(user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(User authenticatedUser, UpdateProfileRequest request) {
        log.info("Atualizando perfil do apoiador: {}", authenticatedUser.getId());

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        // Atualiza campos na entidade User
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        userRepository.save(user);

        // Atualiza ou cria SupporterProfile
        SupporterProfile profile = supporterProfileRepository.findByUserId(user.getId())
                .orElse(SupporterProfile.builder().user(user).build());

        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getBioExtended() != null) {
            profile.setBioExtended(request.getBioExtended());
        }
        if (request.getWebsiteUrl() != null) {
            profile.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }

        supporterProfileRepository.save(profile);

        return getProfile(user.getId());
    }

    @Transactional
    public ProfileResponse uploadAvatar(User authenticatedUser, MultipartFile file) {
        log.info("Upload de avatar para o usuário: {}", authenticatedUser.getId());
        MediaService.MediaUploadResult result = mediaService.uploadAvatar(file, authenticatedUser.getId());

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        user.setAvatarUrl(result.url());
        userRepository.save(user);

        return getProfile(user.getId());
    }

    @Transactional
    public ProfileResponse uploadCover(User authenticatedUser, MultipartFile file) {
        log.info("Upload de capa para o usuário: {}", authenticatedUser.getId());
        MediaService.MediaUploadResult result = mediaService.uploadCover(file, authenticatedUser.getId());

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        user.setCoverUrl(result.url());
        userRepository.save(user);

        return getProfile(user.getId());
    }
}
