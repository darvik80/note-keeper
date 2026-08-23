package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.model.UserSettings;
import xyz.crearts.note.keeper.service.DailyReportService;
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
    private final DailyReportService dailyReportService;

    public SettingsController(UserSettingsService userSettingsService, JwtService jwtService,
                              DailyReportService dailyReportService) {
        this.userSettingsService = userSettingsService;
        this.jwtService = jwtService;
        this.dailyReportService = dailyReportService;
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

    /**
     * Preview the daily report (rendered text, not sent).
     */
    @GetMapping("/daily-report/preview")
    public ResponseEntity<String> previewDailyReport(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.validateToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        String report = dailyReportService.generateReport(userId);
        return ResponseEntity.ok(report);
    }

    /**
     * Send a test daily report immediately.
     */
    @PostMapping("/daily-report/test")
    public ResponseEntity<Void> testDailyReport(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtService.validateToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        dailyReportService.sendTestReport(userId);
        return ResponseEntity.ok().build();
    }
}
