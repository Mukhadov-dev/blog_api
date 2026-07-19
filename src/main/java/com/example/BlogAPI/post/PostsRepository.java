package com.example.BlogAPI.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PostsRepository extends JpaRepository<Post, Long> {
    Post getPostById(Long id);
    List<Post> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewsCount = :views, p.likesCount = :likes WHERE p.id = :id")
    void updateStats(@Param("id") Long id, @Param("views") Long views, @Param("likes") Long likes);
}
