package xyz.crearts.note.keeper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.crearts.note.keeper.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for database backup and restore operations.
 * Supports export to JSON and import from JSON.
 */
@Slf4j
@Service
public class BackupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Path backupDirPath;

    public BackupService(@Value("${app.storage.backups-dir}") String backupsDir) {
        this.backupDirPath = Path.of(backupsDir);
    }

    /**
     * Export all data to JSON file.
     * @return Path to the backup file
     */
    @Transactional(readOnly = true)
    public Path exportData() throws IOException {
        log.info("Starting database export...");
        
        // Create backup directory if not exists
        if (!Files.exists(backupDirPath)) {
            Files.createDirectories(backupDirPath);
        }

        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupFile = backupDirPath.resolve("backup_" + timestamp + ".json");

        // Export all tables
        Map<String, Object> backup = new LinkedHashMap<>();
        backup.put("exportedAt", LocalDateTime.now().toString());
        backup.put("version", "1.0");

        // Export tables
        backup.put("users", exportTable("users"));
        backup.put("user_credentials", exportTable("user_credentials"));
        backup.put("note", exportTable("note"));
        backup.put("note_history", exportTable("note_history"));
        backup.put("todo", exportTable("todo"));
        backup.put("attachment", exportTable("attachment"));
        backup.put("note_template", exportTable("note_template"));
        backup.put("saved_query", exportTable("saved_query"));
        backup.put("user_settings", exportTable("user_settings"));

        // Write to file
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backup);
        Files.writeString(backupFile, json, StandardCharsets.UTF_8);

        // Also backup attachment files
        backupAttachmentFiles(backupDirPath.resolve("backup_" + timestamp + "_files"));

        log.info("Export completed: {}", backupFile);
        return backupFile;
    }

    /**
     * Export a single table to list of maps.
     */
    private List<Map<String, Object>> exportTable(String tableName) {
        log.debug("Exporting table: {}", tableName);
        try {
            return jdbcTemplate.query(
                "SELECT * FROM " + tableName,
                (rs, rowNum) -> extractRow(rs)
            );
        } catch (Exception e) {
            log.error("Failed to export table {}: {}", tableName, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Extract row data from ResultSet.
     */
    private Map<String, Object> extractRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = rs.getMetaData().getColumnName(i);
            Object value = rs.getObject(i);
            row.put(columnName, value);
        }
        return row;
    }

    /**
     * Import data from JSON file.
     * @param backupFile Path to backup file
     */
    @Transactional
    public void importData(Path backupFile) throws IOException {
        log.info("Starting database import from: {}", backupFile);
        
        if (!Files.exists(backupFile)) {
            throw new IOException("Backup file not found: " + backupFile);
        }

        String json = Files.readString(backupFile, StandardCharsets.UTF_8);
        Map<String, Object> backup = objectMapper.readValue(json, Map.class);

        log.info("Importing data from backup exported at: {}", backup.get("exportedAt"));

        // Import tables in correct order (respecting foreign keys)
        importTable("user_settings", (List<Map<String, Object>>) backup.get("user_settings"));
        importTable("note_template", (List<Map<String, Object>>) backup.get("note_template"));
        importTable("saved_query", (List<Map<String, Object>>) backup.get("saved_query"));
        importTable("users", (List<Map<String, Object>>) backup.get("users"));
        importTable("user_credentials", (List<Map<String, Object>>) backup.get("user_credentials"));
        importTable("note", (List<Map<String, Object>>) backup.get("note"));
        importTable("note_history", (List<Map<String, Object>>) backup.get("note_history"));
        importTable("todo", (List<Map<String, Object>>) backup.get("todo"));
        importTable("attachment", (List<Map<String, Object>>) backup.get("attachment"));

        log.info("Import completed successfully");
    }

    /**
     * Import data into a single table.
     */
    @SuppressWarnings("unchecked")
    private void importTable(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            log.debug("No data to import for table: {}", tableName);
            return;
        }

        log.info("Importing {} rows into table: {}", rows.size(), tableName);
        
        // Disable foreign key checks for import (SQLite only)
        try {
            jdbcTemplate.execute("PRAGMA foreign_keys=OFF");
        } catch (Exception e) {
            // Ignore if not supported (PostgreSQL)
            log.debug("Could not disable foreign keys: {}", e.getMessage());
        }

        for (Map<String, Object> row : rows) {
            try {
                insertRow(tableName, row);
            } catch (Exception e) {
                log.error("Failed to insert row into {}: {}", tableName, e.getMessage());
            }
        }

        // Re-enable foreign key checks (SQLite only)
        try {
            jdbcTemplate.execute("PRAGMA foreign_keys=ON");
        } catch (Exception e) {
            // Ignore if not supported (PostgreSQL)
            log.debug("Could not re-enable foreign keys: {}", e.getMessage());
        }
    }

    /**
     * Insert a single row into table.
     */
    private void insertRow(String tableName, Map<String, Object> row) {
        if (row.isEmpty()) return;

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(entry.getKey());
            placeholders.append("?");
            values.add(entry.getValue());
        }

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        
        try {
            jdbcTemplate.update(sql, values.toArray());
        } catch (Exception e) {
            // Skip duplicate keys
            log.debug("Skipping duplicate row in {}: {}", tableName, e.getMessage());
        }
    }

    /**
     * Get list of available backups.
     */
    public List<Map<String, String>> listBackups() {
        if (!Files.exists(backupDirPath)) {
            return new ArrayList<>();
        }

        try {
            return Files.list(backupDirPath)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("filename", path.getFileName().toString());
                    info.put("path", path.toAbsolutePath().toString());
                    try {
                        info.put("size", Files.size(path) + " bytes");
                        info.put("modified", Files.getLastModifiedTime(path).toString());
                    } catch (IOException e) {
                        // Ignore
                    }
                    return info;
                })
                .sorted((a, b) -> b.get("filename").compareTo(a.get("filename")))
                .toList();
        } catch (IOException e) {
            log.error("Failed to list backups: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Delete a backup file.
     */
    public boolean deleteBackup(String filename) {
        Path backupFile = backupDirPath.resolve(filename);
        try {
            if (Files.exists(backupFile)) {
                Files.delete(backupFile);
                log.info("Deleted backup: {}", filename);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to delete backup {}: {}", filename, e.getMessage());
            return false;
        }
    }

    /**
     * Backup attachment files to a separate directory.
     * Copies all files from ./var/attachments/ to backup directory.
     */
    private void backupAttachmentFiles(Path backupFilesDir) throws IOException {
        String attachmentsDir = System.getProperty("user.dir") + "/var/attachments";
        Path sourceDir = Path.of(attachmentsDir);
        
        if (!Files.exists(sourceDir)) {
            log.info("No attachments directory found, skipping file backup");
            return;
        }

        log.info("Backing up attachment files to: {}", backupFilesDir);
        Files.createDirectories(backupFilesDir);

        // Copy entire attachments directory structure
        copyDirectory(sourceDir, backupFilesDir);
        
        log.info("Attachment files backup completed");
    }

    /**
     * Recursively copy a directory.
     */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        
        Files.list(source).forEach(path -> {
            Path targetPath = target.resolve(source.relativize(path));
            try {
                if (Files.isDirectory(path)) {
                    copyDirectory(path, targetPath);
                } else {
                    Files.copy(path, targetPath);
                    log.debug("Copied file: {}", targetPath);
                }
            } catch (IOException e) {
                log.error("Failed to copy file: {}", path, e);
            }
        });
    }
}
