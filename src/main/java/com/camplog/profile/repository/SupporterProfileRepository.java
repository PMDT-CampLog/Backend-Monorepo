package com.camplog.profile.repository;

import com.camplog.profile.model.SupporterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupporterProfileRepository extends JpaRepository<SupporterProfile, String> {
    Optional<SupporterProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
