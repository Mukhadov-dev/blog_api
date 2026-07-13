package com.example.BlogAPI.user;

import com.example.BlogAPI.user.dto.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    List<User> findByUsernameContainingIgnoreCase(String username);
    List<UserResponse> findByRolesContaining(UserRole role);
}
