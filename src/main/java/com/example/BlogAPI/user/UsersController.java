package com.example.BlogAPI.user;

import com.example.BlogAPI.sub.SubscriptionsService;
import com.example.BlogAPI.user.dto.UserRequest;
import com.example.BlogAPI.user.dto.UserResponse;
import com.example.BlogAPI.user.dto.UserUpdate;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UsersService usersService;
    private final SubscriptionsService subscriptionsService;

    @Autowired
    public UsersController(UsersService usersService, SubscriptionsService subscriptionsService) {
        this.usersService = usersService;
        this.subscriptionsService = subscriptionsService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = usersService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = usersService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest userRequest,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>("Cannot create a user", HttpStatus.BAD_REQUEST);
        }

        UserResponse createdUser = usersService.createUser(userRequest);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdate userUpdate, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>("Cannot update a user", HttpStatus.BAD_REQUEST);
        }

        UserResponse updatedUser = usersService.updateUser(id, userUpdate);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        usersService.deleteUserById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> followUser(@PathVariable Long userId, Authentication authentication) {
        User currentUser = usersService.getCurrentUser(authentication);
        UserResponse userToFollow = usersService.getUserById(userId);

        String username = userToFollow.getUsername();

        subscriptionsService.followUser(currentUser.getId(), userToFollow.getId());

        return ResponseEntity.ok(Map.of("message", "Successfully followed " + username));
    }

    @PostMapping("/{userId}/unfollow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId, Authentication authentication) {
        User currentUser = usersService.getCurrentUser(authentication);
        UserResponse userToUnfollow = usersService.getUserById(userId);

        String username = userToUnfollow.getUsername();

        subscriptionsService.unfollowUser(currentUser.getId(), userToUnfollow.getId());

        return ResponseEntity.ok(Map.of("message", "Successfully unfollowed " + username));
    }



    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String q, Authentication authentication) {
        User currentUser = usersService.getCurrentUser(authentication);
        List<UserResponse> users = usersService.searchUsers(q);

        return ResponseEntity.ok(users);
    }


}
