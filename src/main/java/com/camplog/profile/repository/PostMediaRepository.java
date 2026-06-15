package com.camplog.profile.repository;

import com.camplog.profile.model.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, String> {
    List<PostMedia> findByPostIdOrderByPositionAsc(String postId);
    void deleteByPostId(String postId);
}
