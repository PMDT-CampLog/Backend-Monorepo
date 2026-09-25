package com.camplog.bond.controller;

import com.camplog.bond.dto.ProfileResponse;
import com.camplog.bond.service.MediaService;
import com.camplog.bond.service.ProfileService;
import com.camplog.auth.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/bond")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final MediaService mediaService;

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String userId){
        log.info("Retornando o user: {}", userId);
        ProfileResponse response = profileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/me/avatar")
    @PreAuthorize("hasAnyRole('MEMBER', 'APOIADOR')")
    public ResponseEntity<ProfileResponse> updateAvatar(
            @AuthenticationPrincipal User user, @RequestParam("file")
            MultipartFile file
    ){
        log.info("Update de avatar: {}", user.getId());
        ProfileResponse profile = profileService.updateAvatar(user, file);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me/cover")
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
