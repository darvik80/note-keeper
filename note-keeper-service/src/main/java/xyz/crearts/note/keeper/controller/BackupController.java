package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.crearts.note.keeper.service.BackupService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Controller for backup/restore operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.storage.backups-dir}")
    private String backupsDir;

    /**
     * Export all data to JSON file (download).
     */
    @GetMapping("/export")
    public ResponseEntity<FileSystemResource> exportData() throws IOException {
        log.info("Export requested");
        Path backupFile = backupService.exportData();
        
        FileSystemResource resource = new FileSystemResource(backupFile.toFile());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + backupFile.getFileName() + "\"")
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(Files.size(backupFile))
            .body(resource);
    }

    /**
     * Import data from uploaded JSON file.
     */
    @PostMapping("/import")
    public Map<String, String> importData(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Import requested: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Save uploaded file temporarily
        Path tempFile = Files.createTempFile("backup_import_", ".json");
        file.transferTo(tempFile);

        try {
            backupService.importData(tempFile);
            return Map.of(
                "status", "success",
                "message", "Data imported successfully"
            );
        } finally {
            // Clean up temp file
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * List all available backups.
     */
    @GetMapping("/list")
    public List<Map<String, String>> listBackups() {
        log.info("List backups requested");
        return backupService.listBackups();
    }

    /**
     * Delete a backup file.
     */
    @DeleteMapping("/delete/{filename}")
    public Map<String, String> deleteBackup(@PathVariable String filename) {
        log.info("Delete backup requested: {}", filename);
        
        boolean deleted = backupService.deleteBackup(filename);
        if (deleted) {
            return Map.of(
                "status", "success",
                "message", "Backup deleted: " + filename
            );
        } else {
            return Map.of(
                "status", "error",
                "message", "Backup not found: " + filename
            );
        }
    }

    /**
     * Download a specific backup file.
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<FileSystemResource> downloadBackup(@PathVariable String filename) throws IOException {
        log.info("Download backup requested: {}", filename);
        
        Path backupFile = Paths.get(backupsDir, filename);
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

    /**
     * Get backup settings.
     */
    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        log.info("Get backup settings requested");
        
        try {
            Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT backup_auto_enabled FROM user_settings WHERE id = 'default'",
                Boolean.class
            );
            
            String cron = jdbcTemplate.queryForObject(
                "SELECT backup_cron FROM user_settings WHERE id = 'default'",
                String.class
            );
            
            Integer retentionDays = jdbcTemplate.queryForObject(
                "SELECT backup_retention_days FROM user_settings WHERE id = 'default'",
                Integer.class
            );

            return Map.of(
                "enabled", enabled != null && enabled,
                "cron", cron != null ? cron : "0 0 2 * * *",
                "retentionDays", retentionDays != null ? retentionDays : 30
            );
        } catch (Exception e) {
            log.error("Failed to get backup settings: {}", e.getMessage());
            return Map.of(
                "enabled", false,
                "cron", "0 0 2 * * *",
                "retentionDays", 30
            );
        }
    }

    /**
     * Update backup settings.
     */
    @PostMapping("/settings")
    public Map<String, String> updateSettings(@RequestBody Map<String, Object> settings) {
        log.info("Update backup settings requested: {}", settings);
        
        try {
            Boolean enabled = (Boolean) settings.get("enabled");
            String cron = (String) settings.get("cron");
            Integer retentionDays = (Integer) settings.get("retentionDays");

            jdbcTemplate.update(
                "INSERT INTO user_settings (id, backup_auto_enabled, backup_cron, backup_retention_days, updated_at) " +
                "VALUES ('default', ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "backup_auto_enabled = ?, " +
                "backup_cron = ?, " +
                "backup_retention_days = ?, " +
                "updated_at = CURRENT_TIMESTAMP",
                enabled, cron, retentionDays, enabled, cron, retentionDays
            );

            return Map.of(
                "status", "success",
                "message", "Backup settings updated successfully"
            );
        } catch (Exception e) {
            log.error("Failed to update backup settings: {}", e.getMessage());
            return Map.of(
                "status", "error",
                "message", "Failed to update backup settings: " + e.getMessage()
            );
        }
    }
}
