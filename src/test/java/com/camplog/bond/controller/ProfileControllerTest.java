package com.camplog.bond.controller;

import com.camplog.auth.model.User;
import com.camplog.auth.config.JwtService;
import com.camplog.auth.repository.UserRepository;
import com.camplog.bond.dto.ProfileResponse;
import com.camplog.bond.service.MediaService;
import com.camplog.pokedex.dto.UpdateProfileRequest;
import com.camplog.bond.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProfileController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MediaService mediaService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProfileResponse profileResponse;

    private UpdateProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        profileResponse = ProfileResponse.builder()
                .userId("user-123")
                .name("Alice")
                .displayName("Alice In Chains")
                .email("alice@camplog.com")
                .avatarUrl("http://cdn/avatar.jpg")
                .coverUrl("http://cdn/cover.jpg")
                .bio("Alice's Bio")
                .role("apoiador")
                .postsCount(10)
                .likesReceivedCount(42)
                .interests(new ArrayList<>())
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

        updateRequest = UpdateProfileRequest.builder()
                .displayName("Alice In Chains Updated")
                .bio("New Bio")
                .bioExtended("Updated extended bio.")
                .websiteUrl("https://newalice.com")
                .location("Portland, OR")
                .build();
    }

    @Test
    void getProfile_validUserId_returnsProfileResponse() throws Exception {
        when(profileService.getProfile("user-123")).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/bond/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.displayName").value("Alice In Chains"))
                .andExpect(jsonPath("$.postsCount").value(10))
                .andExpect(jsonPath("$.likesReceivedCount").value(42));

        verify(profileService, times(1)).getProfile("user-123");
    }

    @Test
    void uploadAvatar_validMultipartFile_returnsProfileResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(profileService.updateAvatar(any(), any())).thenReturn(profileResponse);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/bond/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://cdn/avatar.jpg"));

        verify(profileService, times(1)).updateAvatar(any(), any());
    }

    @Test
    void uploadCover_validMultipartFile_returnsProfileResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
        when(profileService.uploadCover(any(), any())).thenReturn(profileResponse);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/bond/me/cover").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverUrl").value("http://cdn/cover.jpg"));

        verify(profileService, times(1)).uploadCover(any(), any());
    }
}
