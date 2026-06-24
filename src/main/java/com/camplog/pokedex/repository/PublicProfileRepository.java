package com.camplog.pokedex.repository;

import com.camplog.pokedex.model.PublicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicProfileRepository extends JpaRepository<PublicProfile, String> {
    Optional<PublicProfile> findByUsername(String username);
    Optional<PublicProfile> findByUserId(String userId);
    boolean existsByUsername(String username);
}
