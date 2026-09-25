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
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean available = pokedexService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of("available", available));
    }
}
