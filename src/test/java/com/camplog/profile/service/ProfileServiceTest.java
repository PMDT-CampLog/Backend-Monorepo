package com.camplog.profile.service;

import com.camplog.auth.model.User;
import com.camplog.auth.repository.UserRepository;
import com.camplog.profile.dto.ProfileResponse;
import com.camplog.profile.dto.UpdateProfileRequest;
import com.camplog.profile.model.SupporterProfile;
import com.camplog.profile.repository.PostLikeRepository;
import com.camplog.profile.repository.PostRepository;
import com.camplog.profile.repository.SupporterProfileRepository;
import com.camplog.profile.repository.UserInterestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupporterProfileRepository supporterProfileRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private ProfileService profileService;

    private User user;
    private SupporterProfile profile;
    private UpdateProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-123")
                .name("Alice")
                .email("alice@camplog.com")
                .role("apoiador")
                .avatarUrl("http://cdn/avatar.jpg")
                .coverUrl("http://cdn/cover.jpg")
                .bio("Alice's Bio")
                .createdAt(LocalDateTime.now())
                .build();

        profile = SupporterProfile.builder()
                .id("profile-123")
                .user(user)
                .displayName("Alice In Chains")
                .bioExtended("Detailed bio about Alice.")
                .websiteUrl("https://alice.com")
                .location("Seattle, WA")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
    void getProfile_existingUser_returnsProfileResponse() {
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(profile));
        when(postRepository.countByAuthorId("user-123")).thenReturn(10);
        when(postLikeRepository.countTotalLikesReceivedByAuthor("user-123")).thenReturn(42);
        when(userInterestRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        ProfileResponse response = profileService.getProfile("user-123");

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        assertEquals("Alice In Chains", response.getDisplayName());
        assertEquals(10, response.getPostsCount());
        assertEquals(42, response.getLikesReceivedCount());
        assertEquals("Seattle, WA", response.getLocation());
    }

    @Test
    void getProfile_nonExistentUser_throwsNotFound() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            profileService.getProfile("non-existent");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateProfile_existingProfile_updatesFieldsAndReturnsResponse() {
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(profile));
        when(supporterProfileRepository.save(any(SupporterProfile.class))).thenReturn(profile);
        when(postRepository.countByAuthorId("user-123")).thenReturn(10);
        when(postLikeRepository.countTotalLikesReceivedByAuthor("user-123")).thenReturn(42);
        when(userInterestRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        ProfileResponse response = profileService.updateProfile(user, updateRequest);

        assertNotNull(response);
        assertEquals("Alice In Chains Updated", response.getDisplayName());
        assertEquals("New Bio", user.getBio());
        verify(userRepository, times(1)).save(user);
        verify(supporterProfileRepository, times(1)).save(profile);
    }

    @Test
    void uploadAvatar_validFile_uploadsAndUpdatesUser() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
        MediaService.MediaUploadResult uploadResult = new MediaService.MediaUploadResult("http://cdn/avatar-new.jpg", "avatar-key");

        when(mediaService.uploadAvatar(any(MultipartFile.class), eq("user-123"))).thenReturn(uploadResult);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(profile));
        when(postRepository.countByAuthorId("user-123")).thenReturn(10);
        when(postLikeRepository.countTotalLikesReceivedByAuthor("user-123")).thenReturn(42);
        when(userInterestRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        ProfileResponse response = profileService.uploadAvatar(user, file);

        assertNotNull(response);
        assertEquals("http://cdn/avatar-new.jpg", response.getAvatarUrl());
        verify(mediaService, times(1)).uploadAvatar(file, "user-123");
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void uploadCover_validFile_uploadsAndUpdatesUser() {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
        MediaService.MediaUploadResult uploadResult = new MediaService.MediaUploadResult("http://cdn/cover-new.jpg", "cover-key");

        when(mediaService.uploadCover(any(MultipartFile.class), eq("user-123"))).thenReturn(uploadResult);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(supporterProfileRepository.findByUserId("user-123")).thenReturn(Optional.of(profile));
        when(postRepository.countByAuthorId("user-123")).thenReturn(10);
        when(postLikeRepository.countTotalLikesReceivedByAuthor("user-123")).thenReturn(42);
        when(userInterestRepository.findByUserId("user-123")).thenReturn(new ArrayList<>());

        ProfileResponse response = profileService.uploadCover(user, file);

        assertNotNull(response);
        assertEquals("http://cdn/cover-new.jpg", response.getCoverUrl());
        verify(mediaService, times(1)).uploadCover(file, "user-123");
        verify(userRepository, times(1)).save(user);
    }
}
