package com.camplog.pokedex.controller;

import com.camplog.auth.model.User;
import com.camplog.pokedex.dto.PublicProfileDto;
import com.camplog.pokedex.dto.UpdatePublicProfileDto;
import com.camplog.pokedex.service.PokedexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pokedex")
@RequiredArgsConstructor
public class PokedexController {

    private final PokedexService pokedexService;

    @GetMapping("/{username}")
    public ResponseEntity<PublicProfileDto> getProfileByUsername(@PathVariable String username) {
        return ResponseEntity.ok(pokedexService.getProfileByUsername(username));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PublicProfileDto> getProfileByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(pokedexService.getProfileByUserId(userId));
    }

    @GetMapping("/check/username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String q) {
        boolean available = pokedexService.isUsernameAvailable(q);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PutMapping("/me")
    public ResponseEntity<Void> updateMyProfile(
            @AuthenticationPrincipal User user,
            @RequestPart(value = "data", required = false) UpdatePublicProfileDto dto,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            @RequestPart(value = "cover", required = false) MultipartFile cover
    ) {
        String userId = user.getId();

        if (dto != null) {
            pokedexService.createOrUpdateProfileSync(userId, dto);
        }

        if ((avatar != null && !avatar.isEmpty()) || (cover != null && !cover.isEmpty())) {
            // Process media asynchronously, returning 202 immediately to frontend
            pokedexService.processMediaUploadAsync(userId, avatar, cover);
        }

        return ResponseEntity.accepted().build();
    }
}
