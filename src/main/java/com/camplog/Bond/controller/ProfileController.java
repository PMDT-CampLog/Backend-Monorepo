package com.camplog.Bond.controller;

import com.camplog.Bond.dto.ProfileResponse;
import com.camplog.Bond.service.MediaService;
import com.camplog.Bond.service.ProfileService;
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

    @PutMapping("/me/update/avatar")
    @PreAuthorize("hasAnyRole('MEMBER', 'APOIADOR')")
    public ResponseEntity<Void> updateAvatar(
            @AuthenticationPrincipal User user, @RequestParam("file")
            MultipartFile file
    ){
        log.info("Update de avatar: {}", user.getId());
        profileService.ReplaceAvatar(user, file);
        return ResponseEntity.ok().build();
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
