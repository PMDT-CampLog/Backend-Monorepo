package com.camplog.pokedex.controller;

import com.camplog.pokedex.model.SpotifyConnection;
import com.camplog.pokedex.repository.SpotifyConnectionRepository;
import com.camplog.pokedex.service.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;
    private final SpotifyConnectionRepository connectionRepository;

    public SpotifyController(SpotifyService spotifyService, SpotifyConnectionRepository connectionRepository) {
        this.spotifyService = spotifyService;
        this.connectionRepository = connectionRepository;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("url", spotifyService.getAuthorizationUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(@RequestParam("userId") String userId, @RequestParam("code") String code) {
        spotifyService.exchangeCodeForTokens(userId, code);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@RequestParam("userId") String userId) {
        spotifyService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/preferences")
    public ResponseEntity<Void> setPreferences(@RequestParam("userId") String userId, @RequestParam(value = "trackId", required = false) String trackId) {
        spotifyService.setFixedTrack(userId, trackId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable("userId") String userId) {
        Map<String, Object> response = new HashMap<>();
        Optional<SpotifyConnection> connectionOpt = connectionRepository.findByUserId(userId);
        
        if (connectionOpt.isEmpty()) {
            response.put("connected", false);
            return ResponseEntity.ok(response);
        }
        
        SpotifyConnection connection = connectionOpt.get();
        response.put("connected", true);
        
        if (connection.getFixedTrackId() != null && !connection.getFixedTrackId().isEmpty()) {
            response.put("trackId", connection.getFixedTrackId());
            response.put("mode", "FIXED");
        } else {
            Optional<String> currentlyPlaying = spotifyService.getCurrentlyPlayingTrackId(userId);
            if (currentlyPlaying.isPresent()) {
                response.put("trackId", currentlyPlaying.get());
                response.put("mode", "LIVE");
            } else {
                response.put("trackId", null);
                response.put("mode", "NONE");
            }
        }
        
        return ResponseEntity.ok(response);
    }
}
