package com.example.BlogAPI.user;

import com.example.BlogAPI.kafka.events.UserDeletedEvent;
import com.example.BlogAPI.kafka.events.UserRegisteredEvent;
import com.example.BlogAPI.kafka.events.UserUpdatedEvent;
import com.example.BlogAPI.user.dto.UserRequest;
import com.example.BlogAPI.user.dto.UserResponse;
import com.example.BlogAPI.user.dto.UserShortResponse;
import com.example.BlogAPI.user.dto.UserUpdate;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsersService {
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    public List<UserShortResponse> getAllUsers() {
        return usersRepository.findAll().stream()
                .map(this::convertToUserShortResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        return convertToUserResponse(usersRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id)));
    }

    public UserResponse getUserByUsername(String username) {
        return convertToUserResponse(usersRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User with username " + username + " not found!")));
    }

    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        log.info("Creating new user: {}", userRequest.getUsername());

        User user = convertToUser(userRequest);
        User savedUser = usersRepository.save(user);

        eventPublisher.publishEvent(buildUserRegisteredEvent(savedUser));
        log.info("User created, event published: userId={}", savedUser.getId());

        return convertToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdate userUpdate, Authentication authentication) {
        log.info("Updating user: {}", id);

        User userToUpdate = usersRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + id + " not found!"));

        if (!userToUpdate.getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only edit your own profile");
        }

        userToUpdate.setUsername(userUpdate.getUsername());

        eventPublisher.publishEvent(buildUserUpdatedEvent(userToUpdate));
        log.info("User updated, event published: userId={}", userToUpdate.getId());

        return convertToUserResponse(userToUpdate);
    }

    @Transactional
    public void deleteUser(Long id, Authentication authentication) {
        User user = usersRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + id + " not found!"));

        if  (!user.getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only delete your own profile");
        }

        usersRepository.delete(user);
        eventPublisher.publishEvent(new UserDeletedEvent(id));
        log.info("User deleted, event published: userId={}", id);
    }

    public List<UserResponse> searchUsers(String query) {
        return usersRepository.findByUsernameContainingIgnoreCase(query).stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String username = authentication.getName();
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
    }

    private UserRegisteredEvent buildUserRegisteredEvent(User user) {
        return new UserRegisteredEvent(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt());
    }

    private UserUpdatedEvent buildUserUpdatedEvent(User user) {
        return new UserUpdatedEvent(
                user.getId(),
                user.getUsername(),
                user.getUpdatedAt()
        );
    }

    private User convertToUser(UserRequest userRequest) {
        return modelMapper.map(userRequest, User.class);
    }

    private UserResponse convertToUserResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }

    private UserShortResponse convertToUserShortResponse(User user) {
        return modelMapper.map(user, UserShortResponse.class);
    }
}
