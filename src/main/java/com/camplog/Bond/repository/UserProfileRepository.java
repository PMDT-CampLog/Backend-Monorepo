package com.camplog.Bond.repository;

import com.camplog.Bond.model.UserProfile;

import java.util.Optional;

public interface UserProfileRepository {
    Optional<UserProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
