package com.camplog.profile.controller;

import com.camplog.auth.model.User;
import com.camplog.profile.dto.UpdateInterestsRequest;
import com.camplog.profile.model.UserInterest;
import com.camplog.profile.repository.UserInterestRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class InterestController {

    private final UserInterestRepository userInterestRepository;

    @GetMapping("/{userId}/interests")
    public ResponseEntity<List<String>> getInterests(@PathVariable String userId) {
        List<String> tags = userInterestRepository.findByUserId(userId).stream()
                .map(UserInterest::getTag)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tags);
    }

    @PutMapping("/me/interests")
    @PreAuthorize("hasRole('APOIADOR')")
    @Transactional
    public ResponseEntity<List<String>> updateInterests(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateInterestsRequest request
    ) {
        log.info("Atualizando interesses do apoiador: {}", user.getId());

        // Remove todos os interesses existentes e substitui pela nova lista
        userInterestRepository.deleteByUserId(user.getId());

        List<UserInterest> newInterests = request.getTags().stream()
                .map(tag -> UserInterest.builder()
                        .user(user)
                        .tag(tag.trim())
                        .build())
                .collect(Collectors.toList());

        userInterestRepository.saveAll(newInterests);

        List<String> savedTags = newInterests.stream()
                .map(UserInterest::getTag)
                .collect(Collectors.toList());

        return ResponseEntity.ok(savedTags);
    }
}
