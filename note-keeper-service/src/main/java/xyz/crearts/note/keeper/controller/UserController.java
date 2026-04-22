package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.mapper.UserMapper;
import xyz.crearts.note.keeper.model.User;
import xyz.crearts.note.keeper.service.AuthService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User controller for profile management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

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

    /**
     * Get user by ID.
     */
    @GetMapping("/{id}")
    public User getUserById(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User currentUser = authService.validateToken(token);
        if (currentUser == null) {
            throw new RuntimeException("Invalid token");
        }

        log.info("Getting user by id: {}", id);
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("User not found: " + id);
        }
        return user;
    }

    /**
     * Get users by IDs (batch).
     */
    @PostMapping("/batch")
    public List<User> getUsersByIds(
            @RequestBody List<String> ids,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User currentUser = authService.validateToken(token);
        if (currentUser == null) {
            throw new RuntimeException("Invalid token");
        }

        log.info("Getting users by ids: {}", ids);
        List<User> users = userMapper.findByIds(ids);
        return users;
    }

    /**
     * Search users by email or name (exclude current user).
     */
    @GetMapping("/search")
    public List<User> searchUsers(
            @RequestParam String query,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User currentUser = authService.validateToken(token);
        if (currentUser == null) {
            throw new RuntimeException("Invalid token");
        }

        log.info("Searching users with query: {}, excluding: {}", query, currentUser.getId());
        List<User> users = userMapper.searchByEmailOrName(query, currentUser.getId());
        return users;
    }
}
