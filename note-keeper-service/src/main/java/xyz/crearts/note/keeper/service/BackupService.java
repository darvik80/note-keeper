package xyz.crearts.note.keeper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.crearts.note.keeper.exception.AccessDeniedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    @Transactional(readOnly = true)
    public Path exportData(String userId) throws IOException {
        log.info("Starting database export for user {}", userId);

        Path userBackupDir = backupDirPath.resolve(userId);
        Files.createDirectories(userBackupDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupFile = userBackupDir.resolve("backup_" + timestamp + ".json");

        Map<String, Object> backup = new LinkedHashMap<>();
        backup.put("exportedAt", LocalDateTime.now().toString());
        backup.put("version", "1.1");
        backup.put("ownerId", userId);
        backup.put("note", exportTableWhere("note", "owner_id = ?", userId));
        backup.put("note_history", exportNoteHistory(userId));
        backup.put("todo", exportTableWhere("todo", "owner_id = ?", userId));
        backup.put("attachment", exportAttachments(userId));
        backup.put("saved_query", exportTableWhere("saved_query", "owner_id = ?", userId));
        backup.put("note_template", exportTableWhere("note_template", "owner_id = ?", userId));
        backup.put("user_settings", exportTableWhere("user_settings", "id = ?", userId));

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backup);
        Files.writeString(backupFile, json, StandardCharsets.UTF_8);

        backupUserAttachmentFiles(userId, userBackupDir.resolve("backup_" + timestamp + "_files"));

        log.info("Export completed for user {}: {}", userId, backupFile);
        return backupFile;
    }

    private List<Map<String, Object>> exportTableWhere(String tableName, String whereClause, Object... args) {
        log.debug("Exporting table {} for user scope", tableName);
        try {
            return jdbcTemplate.query(
                "SELECT * FROM " + tableName + " WHERE " + whereClause,
                (rs, rowNum) -> extractRow(rs),
                args
            );
        } catch (Exception e) {
            log.error("Failed to export table {}: {}", tableName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> exportNoteHistory(String userId) {
        return jdbcTemplate.query(
            "SELECT nh.* FROM note_history nh INNER JOIN note n ON nh.note_id = n.id WHERE n.owner_id = ?",
            (rs, rowNum) -> extractRow(rs),
            userId
        );
    }

    private List<Map<String, Object>> exportAttachments(String userId) {
        return jdbcTemplate.query(
            """
            SELECT a.* FROM attachment a
            WHERE (a.parent_type = 'note' AND a.parent_id IN (SELECT id FROM note WHERE owner_id = ?))
               OR (a.parent_type = 'todo' AND a.parent_id IN (SELECT id FROM todo WHERE owner_id = ?))
            """,
            (rs, rowNum) -> extractRow(rs),
            userId, userId
        );
    }

    private Map<String, Object> extractRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
        }
        return row;
    }

    @Transactional
    public void importData(String userId, Path backupFile) throws IOException {
        log.info("Starting database import for user {} from: {}", userId, backupFile);

        if (!Files.exists(backupFile)) {
            throw new IOException("Backup file not found: " + backupFile);
        }

        String json = Files.readString(backupFile, StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> backup = objectMapper.readValue(json, Map.class);

        Object backupOwner = backup.get("ownerId");
        if (backupOwner != null && !userId.equals(String.valueOf(backupOwner))) {
            throw new AccessDeniedException("Backup belongs to another user");
        }

        log.info("Importing user-scoped data from backup exported at: {}", backup.get("exportedAt"));

        importUserTable("user_settings", castRows(backup.get("user_settings")), userId, "id");
        importUserTable("saved_query", castRows(backup.get("saved_query")), userId, "owner_id");
        importUserTable("note_template", castRows(backup.get("note_template")), userId, "owner_id");
        importUserTable("note", castRows(backup.get("note")), userId, "owner_id");
        importUserTable("note_history", castRows(backup.get("note_history")), userId, null);
        importUserTable("todo", castRows(backup.get("todo")), userId, "owner_id");
        importUserTable("attachment", castRows(backup.get("attachment")), userId, null);

        log.info("Import completed successfully for user {}", userId);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castRows(Object rows) {
        if (rows instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    private void importUserTable(String tableName, List<Map<String, Object>> rows, String userId, String ownerField) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        log.info("Importing {} rows into table {} for user {}", rows.size(), tableName, userId);

        try {
            jdbcTemplate.execute("PRAGMA foreign_keys=OFF");
        } catch (Exception ignored) {
        }

        for (Map<String, Object> row : rows) {
            if (ownerField != null) {
                row.put(ownerField, userId);
            }
            try {
                insertRow(tableName, row);
            } catch (Exception e) {
                log.error("Failed to insert row into {}: {}", tableName, e.getMessage());
            }
        }

        try {
            jdbcTemplate.execute("PRAGMA foreign_keys=ON");
        } catch (Exception ignored) {
        }
    }

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
            log.debug("Skipping duplicate row in {}: {}", tableName, e.getMessage());
        }
    }

    public List<Map<String, String>> listBackups(String userId) {
        Path userBackupDir = backupDirPath.resolve(userId);
        if (!Files.exists(userBackupDir)) {
            return new ArrayList<>();
        }

        try {
            return Files.list(userBackupDir)
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("filename", path.getFileName().toString());
                    info.put("path", path.toAbsolutePath().toString());
                    try {
                        info.put("size", Files.size(path) + " bytes");
                        info.put("modified", Files.getLastModifiedTime(path).toString());
                    } catch (IOException ignored) {
                    }
                    return info;
                })
                .sorted((a, b) -> b.get("filename").compareTo(a.get("filename")))
                .toList();
        } catch (IOException e) {
            log.error("Failed to list backups for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean deleteBackup(String userId, String filename) {
        Path backupFile = resolveUserBackupFile(userId, filename);
        try {
            if (Files.exists(backupFile)) {
                Files.delete(backupFile);
                log.info("Deleted backup for user {}: {}", userId, filename);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to delete backup {} for user {}: {}", filename, userId, e.getMessage());
            return false;
        }
    }

    public Path resolveUserBackupFile(String userId, String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid backup filename");
        }
        return backupDirPath.resolve(userId).resolve(filename).normalize();
    }

    private void backupUserAttachmentFiles(String userId, Path backupFilesDir) throws IOException {
        Path sourceDir = Path.of(System.getProperty("user.dir")).resolve("var/attachments").resolve(userId);
        if (!Files.exists(sourceDir)) {
            log.info("No attachment files for user {}, skipping file backup", userId);
            return;
        }

        Files.createDirectories(backupFilesDir);
        copyDirectory(sourceDir, backupFilesDir);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        Files.list(source).forEach(path -> {
            Path targetPath = target.resolve(source.relativize(path));
            try {
                if (Files.isDirectory(path)) {
                    copyDirectory(path, targetPath);
                } else {
                    Files.copy(path, targetPath);
                }
            } catch (IOException e) {
                log.error("Failed to copy file: {}", path, e);
            }
        });
    }
}
