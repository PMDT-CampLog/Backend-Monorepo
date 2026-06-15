package com.camplog.profile.repository;

import com.camplog.profile.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, String> {
    List<UserInterest> findByUserId(String userId);
    void deleteByUserId(String userId);
}
