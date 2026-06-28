package com.camplog.pokedex.service;

import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import com.camplog.pokedex.model.SpotifyConnection;
import com.camplog.pokedex.repository.SpotifyConnectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SpotifyService {

    @Value("${spotify.client.id:cbd21b74d3b54214978c8d73f0709ef7}")
    private String clientId;

    @Value("${spotify.client.secret:ea97f9eb3c094e2b9ca4e2cdbdd21f4f}")
    private String clientSecret;

    @Value("${spotify.redirect.uri:http://localhost:3000/perfil/spotify/callback}")
    private String redirectUri;

    private final SpotifyConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpotifyService(SpotifyConnectionRepository connectionRepository, UserRepository userRepository) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
    }

    public String getAuthorizationUrl() {
        String scopes = "user-read-currently-playing";
        return "https://accounts.spotify.com/authorize?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&scope=" + scopes +
                "&redirect_uri=" + redirectUri;
    }

    @Transactional
    public void exchangeCodeForTokens(String userId, String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = clientId + ":" + clientSecret;
            headers.setBasicAuth(Base64.getEncoder().encodeToString(auth.getBytes()));

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "authorization_code");
            map.add("code", code);
            map.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String accessToken = root.get("access_token").asText();
            String refreshToken = root.has("refresh_token") ? root.get("refresh_token").asText() : null;
            int expiresIn = root.get("expires_in").asInt();

            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            
            SpotifyConnection connection = connectionRepository.findByUserId(userId).orElse(new SpotifyConnection());
            connection.setUser(user);
            connection.setAccessToken(accessToken);
            if (refreshToken != null) {
                connection.setRefreshToken(refreshToken);
            }
            connection.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60)); // 60s buffer
            
            connectionRepository.save(connection);
            
            sendEventToDataPlatform(userId, "LINKED", null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for tokens", e);
        }
    }

    @Transactional
    public void refreshAccessToken(SpotifyConnection connection) {
        if (connection.getExpiresAt().isAfter(LocalDateTime.now())) {
            return;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = clientId + ":" + clientSecret;
            headers.setBasicAuth(Base64.getEncoder().encodeToString(auth.getBytes()));

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "refresh_token");
            map.add("refresh_token", connection.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String newAccessToken = root.get("access_token").asText();
            int expiresIn = root.get("expires_in").asInt();

            connection.setAccessToken(newAccessToken);
            connection.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60));
            connectionRepository.save(connection);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }

    @Transactional
    public void disconnect(String userId) {
        connectionRepository.deleteByUserId(userId);
        sendEventToDataPlatform(userId, "UNLINKED", null);
    }

    @Transactional
    public void setFixedTrack(String userId, String trackId) {
        SpotifyConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Spotify not connected"));
        connection.setFixedTrackId(trackId);
        connectionRepository.save(connection);
        if (trackId != null) {
            sendEventToDataPlatform(userId, "TRACK_PINNED", trackId);
        }
    }
    
    public Optional<String> getCurrentlyPlayingTrackId(String userId) {
        SpotifyConnection connection = connectionRepository.findByUserId(userId).orElse(null);
        if (connection == null) return Optional.empty();
        
        refreshAccessToken(connection);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(connection.getAccessToken());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/me/player/currently-playing", 
                    HttpMethod.GET, 
                    request, 
                    String.class);
                    
            if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null) {
                return Optional.empty();
            }
            
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("item") && !root.get("item").isNull()) {
                return Optional.of(root.get("item").get("id").asText());
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    private void sendEventToDataPlatform(String userId, String eventType, String trackId) {
        try {
            String dataPlatformUrl = "http://localhost:8000/api/v1/events/spotify";
            String signature = "camp-log-data-sec-123";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-CampLog-Signature", signature);
            
            Map<String, String> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("eventType", eventType);
            if (trackId != null) payload.put("trackId", trackId);
            payload.put("timestamp", LocalDateTime.now().toString());
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(dataPlatformUrl, request, String.class);
        } catch (Exception e) {
            System.err.println("Failed to send Spotify event to Data Platform: " + e.getMessage());
        }
    }
}
