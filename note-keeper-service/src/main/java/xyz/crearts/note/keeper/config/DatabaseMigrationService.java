package xyz.crearts.note.keeper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class DatabaseMigrationService {

    private static final String DEFAULT_OWNER_EMAIL = "darvik80@gmail.com";

    private final JdbcTemplate jdbcTemplate;
    private final boolean postgres;

    public DatabaseMigrationService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.postgres = detectPostgres(dataSource);
    }

    public void runPendingMigrations() {
        ensureMigrationTable();
        apply("001_fix_owner_id", this::fixOwnerId);
        apply("002_user_tag", this::addUserTagTable);
        apply("003_backup_settings", this::addBackupSettingsColumns);
        apply("004_saved_query_owner", this::addSavedQueryOwnerColumn);
        apply("005_template_owner", this::addTemplateOwnerColumn);
        apply("006_assign_orphan_records", this::assignOrphanRecordsToDefaultOwner);
    }

    private void apply(String id, Runnable migration) {
        if (isApplied(id)) {
            log.debug("Migration already applied: {}", id);
            return;
        }
        log.info("Applying migration: {}", id);
        migration.run();
        markApplied(id);
        log.info("Migration applied: {}", id);
    }

    private void ensureMigrationTable() {
        if (postgres) {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS schema_migration (
                    id VARCHAR(128) PRIMARY KEY,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        } else {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS schema_migration (
                    id TEXT PRIMARY KEY,
                    applied_at TEXT NOT NULL
                )
                """);
        }
    }

    private boolean isApplied(String id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_migration WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    private void markApplied(String id) {
        jdbcTemplate.update(
                "INSERT INTO schema_migration (id, applied_at) VALUES (?, ?)",
                id,
                LocalDateTime.now().toString()
        );
    }

    private void fixOwnerId() {
        jdbcTemplate.update("""
            DELETE FROM note_history WHERE note_id IN (
                SELECT id FROM note WHERE owner_id IS NULL
            )
            """);
        jdbcTemplate.update("""
            DELETE FROM attachment WHERE parent_type = 'note'
            AND parent_id IN (SELECT id FROM note WHERE owner_id IS NULL)
            """);
        jdbcTemplate.update("DELETE FROM note WHERE owner_id IS NULL");
        jdbcTemplate.update("""
            DELETE FROM attachment WHERE parent_type = 'todo'
            AND parent_id IN (SELECT id FROM todo WHERE owner_id IS NULL)
            """);
        jdbcTemplate.update("DELETE FROM todo WHERE owner_id IS NULL");
    }

    private void addUserTagTable() {
        if (postgres) {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_tag (
                    id VARCHAR(255) PRIMARY KEY,
                    owner_id VARCHAR(255) NOT NULL REFERENCES users(id),
                    tag_name VARCHAR(255) NOT NULL,
                    UNIQUE(owner_id, tag_name)
                )
                """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_tag_owner ON user_tag(owner_id)");
        } else {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_tag (
                    id TEXT PRIMARY KEY,
                    owner_id TEXT NOT NULL REFERENCES users(id),
                    tag_name TEXT NOT NULL,
                    UNIQUE(owner_id, tag_name)
                )
                """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_tag_owner ON user_tag(owner_id)");
        }
    }

    private void addBackupSettingsColumns() {
        addColumnIfMissing("user_settings", "backup_auto_enabled",
                postgres
                        ? "ALTER TABLE user_settings ADD COLUMN backup_auto_enabled BOOLEAN NOT NULL DEFAULT FALSE"
                        : "ALTER TABLE user_settings ADD COLUMN backup_auto_enabled INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing("user_settings", "backup_cron",
                "ALTER TABLE user_settings ADD COLUMN backup_cron TEXT DEFAULT '0 0 2 * * *'");
        addColumnIfMissing("user_settings", "backup_retention_days",
                postgres
                        ? "ALTER TABLE user_settings ADD COLUMN backup_retention_days INTEGER NOT NULL DEFAULT 30"
                        : "ALTER TABLE user_settings ADD COLUMN backup_retention_days INTEGER NOT NULL DEFAULT 30");
    }

    private void addSavedQueryOwnerColumn() {
        addColumnIfMissing("saved_query", "owner_id",
                postgres
                        ? "ALTER TABLE saved_query ADD COLUMN owner_id VARCHAR(255) REFERENCES users(id)"
                        : "ALTER TABLE saved_query ADD COLUMN owner_id TEXT REFERENCES users(id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_saved_query_owner ON saved_query(owner_id)");
    }

    private void addTemplateOwnerColumn() {
        addColumnIfMissing("note_template", "owner_id",
                postgres
                        ? "ALTER TABLE note_template ADD COLUMN owner_id VARCHAR(255) REFERENCES users(id)"
                        : "ALTER TABLE note_template ADD COLUMN owner_id TEXT REFERENCES users(id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_note_template_owner ON note_template(owner_id)");
    }

    private void assignOrphanRecordsToDefaultOwner() {
        List<String> userIds = jdbcTemplate.query(
                "SELECT id FROM users WHERE email = ? LIMIT 1",
                (rs, rowNum) -> rs.getString("id"),
                DEFAULT_OWNER_EMAIL
        );
        if (userIds.isEmpty()) {
            log.info("User {} not found — skipping orphan record assignment", DEFAULT_OWNER_EMAIL);
            return;
        }
        String ownerId = userIds.getFirst();
        int templates = jdbcTemplate.update(
                "UPDATE note_template SET owner_id = ? WHERE owner_id IS NULL",
                ownerId
        );
        int queries = jdbcTemplate.update(
                "UPDATE saved_query SET owner_id = ? WHERE owner_id IS NULL",
                ownerId
        );
        log.info(
                "Assigned {} template(s) and {} saved query/queries to {}",
                templates, queries, DEFAULT_OWNER_EMAIL
        );
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        if (columnExists(table, column)) {
            log.debug("Column {}.{} already exists", table, column);
            return;
        }
        jdbcTemplate.execute(ddl);
    }

    private boolean columnExists(String table, String column) {
        if (postgres) {
            Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_name = ? AND column_name = ?
                """, Integer.class, table.toLowerCase(Locale.ROOT), column.toLowerCase(Locale.ROOT));
            return count != null && count > 0;
        }
        List<String> columns = jdbcTemplate.query(
                "PRAGMA table_info(" + table + ")",
                (rs, rowNum) -> rs.getString("name")
        );
        return columns.stream().anyMatch(column::equalsIgnoreCase);
    }

    private static boolean detectPostgres(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (Exception e) {
            return false;
        }
    }
}
