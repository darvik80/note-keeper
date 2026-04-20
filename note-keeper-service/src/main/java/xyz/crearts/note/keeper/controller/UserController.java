package xyz.crearts.note.keeper.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.mapper.UserMapper;
import xyz.crearts.note.keeper.model.User;
import xyz.crearts.note.keeper.service.AuthService;

import java.time.LocalDateTime;

/**
 * User controller for profile management.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final AuthService authService;
    private final UserMapper userMapper;

    public UserController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    /**
     * Get current user profile.
     */
    @GetMapping("/me")
    public User getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = authService.validateToken(token);
        if (user == null) {
            throw new RuntimeException("Invalid token");
        }
        return user;
    }

    /**
     * Update user profile.
     */
    @PutMapping("/me")
    public User updateProfile(@RequestHeader("Authorization") String authHeader,
                              @RequestBody User userUpdate) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User currentUser = authService.validateToken(token);
        if (currentUser == null) {
            throw new RuntimeException("Invalid token");
        }

        // Update user fields
        currentUser.setName(userUpdate.getName());
        currentUser.setAvatarUrl(userUpdate.getAvatarUrl());
        currentUser.setUpdatedAt(LocalDateTime.now());
        userMapper.update(currentUser);

        return currentUser;
    }
}
