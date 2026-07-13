package com.example.BlogAPI.comment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentariesRepository extends JpaRepository<Commentary, Long> {

    @EntityGraph(attributePaths = {"post", "user"})
    List<Commentary> findByPostId(Long postId);

    @EntityGraph(attributePaths = {"user", "post"})
    @Query("SELECT c FROM Commentary c WHERE c.id = :id")
    Optional<Commentary> findByIdWithPostAndUser(@Param("id") Long id);
}
