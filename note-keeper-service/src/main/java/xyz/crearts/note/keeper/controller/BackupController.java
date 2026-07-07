package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.crearts.note.keeper.service.BackupService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/export")
    public ResponseEntity<FileSystemResource> exportData(@AuthenticationPrincipal String userId) throws IOException {
        log.info("Export requested by user {}", userId);
        Path backupFile = backupService.exportData(userId);

        FileSystemResource resource = new FileSystemResource(backupFile.toFile());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backupFile.getFileName() + "\"")
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(Files.size(backupFile))
            .body(resource);
    }

    @PostMapping("/import")
    public Map<String, String> importData(@RequestParam("file") MultipartFile file,
                                          @AuthenticationPrincipal String userId) throws IOException {
        log.info("Import requested by user {}: {}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Path tempFile = Files.createTempFile("backup_import_", ".json");
        file.transferTo(tempFile);

        try {
            backupService.importData(userId, tempFile);
            return Map.of(
                "status", "success",
                "message", "Data imported successfully"
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @GetMapping("/list")
    public List<Map<String, String>> listBackups(@AuthenticationPrincipal String userId) {
        log.info("List backups requested by user {}", userId);
        return backupService.listBackups(userId);
    }

    @DeleteMapping("/delete/{filename}")
    public Map<String, String> deleteBackup(@PathVariable String filename,
                                            @AuthenticationPrincipal String userId) {
        log.info("Delete backup requested by user {}: {}", userId, filename);

        boolean deleted = backupService.deleteBackup(userId, filename);
        if (deleted) {
            return Map.of(
                "status", "success",
                "message", "Backup deleted: " + filename
            );
        }
        return Map.of(
            "status", "error",
            "message", "Backup not found: " + filename
        );
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<FileSystemResource> downloadBackup(@PathVariable String filename,
                                                             @AuthenticationPrincipal String userId) throws IOException {
        log.info("Download backup requested by user {}: {}", userId, filename);

        Path backupFile = backupService.resolveUserBackupFile(userId, filename);
        if (!Files.exists(backupFile)) {
            throw new IllegalArgumentException("Backup not found: " + filename);
        }

        FileSystemResource resource = new FileSystemResource(backupFile.toFile());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(Files.size(backupFile))
            .body(resource);
    }

    @GetMapping("/settings")
    public Map<String, Object> getSettings(@AuthenticationPrincipal String userId) {
        log.info("Get backup settings requested by user {}", userId);

        try {
            Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT backup_auto_enabled FROM user_settings WHERE id = ?",
                Boolean.class,
                userId
            );

            String cron = jdbcTemplate.queryForObject(
                "SELECT backup_cron FROM user_settings WHERE id = ?",
                String.class,
                userId
            );

            Integer retentionDays = jdbcTemplate.queryForObject(
                "SELECT backup_retention_days FROM user_settings WHERE id = ?",
                Integer.class,
                userId
            );

            return Map.of(
                "enabled", enabled != null && enabled,
                "cron", cron != null ? cron : "0 0 2 * * *",
                "retentionDays", retentionDays != null ? retentionDays : 30
            );
        } catch (Exception e) {
            log.error("Failed to get backup settings for user {}: {}", userId, e.getMessage());
            return Map.of(
                "enabled", false,
                "cron", "0 0 2 * * *",
                "retentionDays", 30
            );
        }
    }

    @PostMapping("/settings")
    public Map<String, String> updateSettings(@RequestBody Map<String, Object> settings,
                                              @AuthenticationPrincipal String userId) {
        log.info("Update backup settings requested by user {}: {}", userId, settings);

        try {
            Boolean enabled = (Boolean) settings.get("enabled");
            String cron = (String) settings.get("cron");
            Integer retentionDays = settings.get("retentionDays") instanceof Number number
                ? number.intValue()
                : 30;

            jdbcTemplate.update(
                """
                INSERT INTO user_settings (id, backup_auto_enabled, backup_cron, backup_retention_days, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO UPDATE SET
                backup_auto_enabled = excluded.backup_auto_enabled,
                backup_cron = excluded.backup_cron,
                backup_retention_days = excluded.backup_retention_days,
                updated_at = CURRENT_TIMESTAMP
                """,
                userId, enabled, cron, retentionDays
            );

            return Map.of(
                "status", "success",
                "message", "Backup settings updated successfully"
            );
        } catch (Exception e) {
            log.error("Failed to update backup settings for user {}: {}", userId, e.getMessage());
            return Map.of(
                "status", "error",
                "message", "Failed to update backup settings: " + e.getMessage()
            );
        }
    }
}
