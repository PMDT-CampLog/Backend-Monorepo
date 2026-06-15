package com.camplog.profile.controller;

import com.camplog.auth.model.User;
import com.camplog.profile.dto.ProfileResponse;
import com.camplog.profile.dto.UpdateProfileRequest;
import com.camplog.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String userId) {
        log.info("Consulta de perfil público para o usuário: {}", userId);
        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'APOIADOR')")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("Atualização de perfil do apoiador: {}", user.getId());
        ProfileResponse profile = profileService.updateProfile(user, request);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/me/avatar")
    @PreAuthorize("hasAnyRole('MEMBER', 'APOIADOR')")
    public ResponseEntity<ProfileResponse> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Upload de avatar para o apoiador: {}", user.getId());
        ProfileResponse profile = profileService.uploadAvatar(user, file);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/me/cover")
    @PreAuthorize("hasAnyRole('MEMBER', 'APOIADOR')")
    public ResponseEntity<ProfileResponse> uploadCover(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Upload de capa para o apoiador: {}", user.getId());
        ProfileResponse profile = profileService.uploadCover(user, file);
        return ResponseEntity.ok(profile);
    }
}
