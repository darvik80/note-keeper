package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import dev.cogitor.note.keeper.model.UserSettings;
import dev.cogitor.note.keeper.service.DailyReportService;
import dev.cogitor.note.keeper.service.UserSettingsService;

/**
 * Controller for managing user settings with encrypted sensitive fields.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserSettingsService userSettingsService;
    private final DailyReportService dailyReportService;

    /**
     * Get current user's settings (sensitive fields are decrypted).
     */
    @GetMapping
    public ResponseEntity<UserSettings> getSettings(@AuthenticationPrincipal String userId) {
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
            @AuthenticationPrincipal String userId,
            @RequestBody UserSettings settings) {
        settings.setId(userId);
        userSettingsService.saveSettings(settings);
        return ResponseEntity.ok().build();
    }

    /**
     * Preview the daily report (rendered text, not sent).
     */
    @GetMapping("/daily-report/preview")
    public ResponseEntity<String> previewDailyReport(@AuthenticationPrincipal String userId) {
        String report = dailyReportService.generateReport(userId);
        return ResponseEntity.ok(report);
    }

    /**
     * Send a test daily report immediately.
     */
    @PostMapping("/daily-report/test")
    public ResponseEntity<Void> testDailyReport(@AuthenticationPrincipal String userId) {
        dailyReportService.sendTestReport(userId);
        return ResponseEntity.ok().build();
    }
}
