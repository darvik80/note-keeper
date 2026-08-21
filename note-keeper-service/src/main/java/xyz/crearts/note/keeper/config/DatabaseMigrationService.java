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
        apply("007_schedule_days_and_repeat", this::expandScheduleRepeat);
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

    private void expandScheduleRepeat() {
        addColumnIfMissing("todo", "schedule_days",
                "ALTER TABLE todo ADD COLUMN schedule_days TEXT");
        if (postgres) {
            jdbcTemplate.execute("ALTER TABLE todo DROP CONSTRAINT IF EXISTS todo_schedule_repeat_check");
            jdbcTemplate.execute("""
                ALTER TABLE todo ADD CONSTRAINT todo_schedule_repeat_check
                CHECK (schedule_repeat IN ('none','daily','weekly','monthly','weekdays','custom'))
                """);
            return;
        }
        // SQLite: rebuild table to expand CHECK on schedule_repeat
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS todo_mig_007 (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                completed INTEGER NOT NULL DEFAULT 0,
                tags TEXT DEFAULT '[]',
                priority TEXT NOT NULL DEFAULT 'medium' CHECK(priority IN ('low','medium','high')),
                is_favorite INTEGER NOT NULL DEFAULT 0,
                is_archived INTEGER NOT NULL DEFAULT 0,
                is_deleted INTEGER NOT NULL DEFAULT 0,
                deleted_at TEXT,
                due_date TEXT,
                reminder TEXT,
                notified_at TEXT,
                notification_channels TEXT,
                location_lat REAL,
                location_lng REAL,
                location_address TEXT,
                schedule_repeat TEXT DEFAULT 'none' CHECK(schedule_repeat IN ('none','daily','weekly','monthly','weekdays','custom')),
                schedule_end_date TEXT,
                schedule_days TEXT,
                owner_id TEXT NOT NULL REFERENCES users(id),
                shared_with TEXT DEFAULT '[]',
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            INSERT INTO todo_mig_007 (
                id, title, description, completed, tags, priority,
                is_favorite, is_archived, is_deleted, deleted_at,
                due_date, reminder, notified_at, notification_channels,
                location_lat, location_lng, location_address,
                schedule_repeat, schedule_end_date, schedule_days,
                owner_id, shared_with, created_at, updated_at
            )
            SELECT
                id, title, description, completed, tags, priority,
                is_favorite, is_archived, is_deleted, deleted_at,
                due_date, reminder, notified_at, notification_channels,
                location_lat, location_lng, location_address,
                schedule_repeat, schedule_end_date, schedule_days,
                owner_id, shared_with, created_at, updated_at
            FROM todo
            """);
        jdbcTemplate.execute("DROP TABLE todo");
        jdbcTemplate.execute("ALTER TABLE todo_mig_007 RENAME TO todo");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_todo_completed ON todo(completed)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_todo_is_deleted ON todo(is_deleted)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_todo_is_archived ON todo(is_archived)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_todo_due_date ON todo(due_date)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_todo_created_at ON todo(created_at)");
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
