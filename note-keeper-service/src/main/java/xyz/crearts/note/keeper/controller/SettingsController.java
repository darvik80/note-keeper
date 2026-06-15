package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.model.UserSettings;
import xyz.crearts.note.keeper.service.JwtService;
import xyz.crearts.note.keeper.service.UserSettingsService;

/**
 * Controller for managing user settings with encrypted sensitive fields.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final UserSettingsService userSettingsService;
    private final JwtService jwtService;

    public SettingsController(UserSettingsService userSettingsService, JwtService jwtService) {
        this.userSettingsService = userSettingsService;
        this.jwtService = jwtService;
    }

    /**
     * Get current user's settings (sensitive fields are decrypted).
     */
    @GetMapping
    public ResponseEntity<UserSettings> getSettings(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.validateToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        UserSettings settings = userSettingsService.getSettings(userId);
        if (settings == null) {
            // Return empty settings object if none exist
            settings = new UserSettings();
            settings.setId(userId);
        }
        return ResponseEntity.ok(settings);
    }

    /**
     * Save current user's settings (sensitive fields will be encrypted).
     */
    @PostMapping
    public ResponseEntity<Void> saveSettings(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserSettings settings) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.validateToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        settings.setId(userId);
        userSettingsService.saveSettings(settings);
        return ResponseEntity.ok().build();
    }
}
