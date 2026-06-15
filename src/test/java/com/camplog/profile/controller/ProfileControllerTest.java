package com.camplog.profile.controller;

import com.camplog.auth.model.User;
import com.camplog.auth.config.JwtService;
import com.camplog.auth.repository.UserRepository;
import com.camplog.profile.dto.ProfileResponse;
import com.camplog.profile.dto.UpdateProfileRequest;
import com.camplog.profile.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

        mockMvc.perform(get("/api/v1/profile/user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.displayName").value("Alice In Chains"))
                .andExpect(jsonPath("$.postsCount").value(10))
                .andExpect(jsonPath("$.likesReceivedCount").value(42));

        verify(profileService, times(1)).getProfile("user-123");
    }

    @Test
    void updateProfile_validRequest_returnsUpdatedProfile() throws Exception {
        when(profileService.updateProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(put("/api/v1/profile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-123"));

        // Note: In tests using excludeAutoConfiguration, @AuthenticationPrincipal resolves to null/default or we mock it.
        // Spring Security is disabled for this test slice, so principal will be null or injected depending on setup.
        verify(profileService, times(1)).updateProfile(any(), any(UpdateProfileRequest.class));
    }

    @Test
    void uploadAvatar_validMultipartFile_returnsProfileResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        when(profileService.uploadAvatar(any(), any())).thenReturn(profileResponse);

        mockMvc.perform(multipart("/api/v1/profile/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://cdn/avatar.jpg"));

        verify(profileService, times(1)).uploadAvatar(any(), any());
    }

    @Test
    void uploadCover_validMultipartFile_returnsProfileResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
        when(profileService.uploadCover(any(), any())).thenReturn(profileResponse);

        mockMvc.perform(multipart("/api/v1/profile/me/cover").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverUrl").value("http://cdn/cover.jpg"));

        verify(profileService, times(1)).uploadCover(any(), any());
    }
}
