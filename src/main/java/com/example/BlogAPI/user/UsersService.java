package com.example.BlogAPI.user;

import com.example.BlogAPI.user.dto.UserRequest;
import com.example.BlogAPI.user.dto.UserResponse;
import com.example.BlogAPI.user.dto.UserUpdate;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UsersService {
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsersService(UsersRepository usersRepository, ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return usersRepository.findAll();
    }

    public UserResponse getUserById(Long id) {
        return convertToUserResponse(usersRepository.findById(id).stream()
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id)));
    }

    public UserResponse getUserByUsername(String username) {
        return convertToUserResponse(usersRepository.findByUsername(username).stream()
                .findAny()
                .orElseThrow(() -> new EntityNotFoundException("User with username " + username + " not found!")));
    }

    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        return convertToUserResponse(usersRepository.save(convertToUser(userRequest)));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdate userUpdate) {
        User userToUpdate = usersRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + id + " not found!"));

        userToUpdate.setUsername(userUpdate.getUsername());

        return convertToUserResponse(usersRepository.save(userToUpdate));
    }

    @Transactional
    public void deleteUserById(Long id) {
        if(!usersRepository.existsById(id)) {
            throw new EntityNotFoundException("User with id " + id + " not found!");
        }
        usersRepository.deleteById(id);
    }

    public List<UserResponse> searchUsers(String query) {
        return usersRepository.findByUsernameContainingIgnoreCase(query);
    }

    // Методы проверки
    public boolean usernameExists(String username) {
        return usersRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return usersRepository.existsByEmail(email);
    }

    public UserResponse updateUserProfile(Long userId, UserRequest request) {
        User user = usersRepository.getUserById(userId);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (emailExists(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        return convertToUserResponse(usersRepository.save(user));
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    private User convertToUser(UserRequest userRequest) {
        return modelMapper.map(userRequest, User.class);
    }

    private UserResponse convertToUserResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }
}
