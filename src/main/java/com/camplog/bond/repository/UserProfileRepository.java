package com.camplog.bond.repository;

import com.camplog.bond.model.UserProfile;
import com.camplog.pokedex.model.PublicProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<PublicProfile, String>{
    Optional<UserProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
