package com.camplog.pokedex.repository;

import com.camplog.pokedex.model.SpotifyConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpotifyConnectionRepository extends JpaRepository<SpotifyConnection, String> {
    Optional<SpotifyConnection> findByUserId(String userId);
    void deleteByUserId(String userId);
}
