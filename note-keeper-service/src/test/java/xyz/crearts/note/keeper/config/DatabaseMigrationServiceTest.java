package xyz.crearts.note.keeper.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseMigrationServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private DatabaseMetaData databaseMetaData;

    private DatabaseMigrationService migrationService;

    @BeforeEach
    void setUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("SQLite");

        migrationService = new DatabaseMigrationService(jdbcTemplate, dataSource);
    }

    @Test
    void runPendingMigrations_shouldApplyAllWhenNoneApplied() {
        when(jdbcTemplate.queryForObject(contains("schema_migration"), eq(Integer.class), any()))
                .thenReturn(0);
        when(jdbcTemplate.query(eq("PRAGMA table_info(saved_query)"), any(RowMapper.class)))
                .thenReturn(List.of("id", "name", "query"));
        when(jdbcTemplate.query(eq("PRAGMA table_info(note_template)"), any(RowMapper.class)))
                .thenReturn(List.of("id", "name", "content"));
        when(jdbcTemplate.query(eq("PRAGMA table_info(user_settings)"), any(RowMapper.class)))
                .thenReturn(List.of("id"));
        when(jdbcTemplate.query(
                eq("SELECT id FROM users WHERE email = ? LIMIT 1"),
                any(RowMapper.class),
                eq("darvik80@gmail.com")
        )).thenReturn(List.of("user-darvik"));

        migrationService.runPendingMigrations();

        verify(jdbcTemplate, atLeastOnce()).execute(matches("(?s).*schema_migration.*"));
        verify(jdbcTemplate, atLeastOnce()).execute(matches("(?s).*ALTER TABLE saved_query ADD COLUMN owner_id.*"));
        verify(jdbcTemplate, atLeastOnce()).execute(matches("(?s).*ALTER TABLE note_template ADD COLUMN owner_id.*"));
        verify(jdbcTemplate).update(
                matches("(?s).*UPDATE note_template SET owner_id = \\? WHERE owner_id IS NULL.*"),
                eq("user-darvik")
        );
        verify(jdbcTemplate).update(
                matches("(?s).*UPDATE saved_query SET owner_id = \\? WHERE owner_id IS NULL.*"),
                eq("user-darvik")
        );
        verify(jdbcTemplate, times(6)).update(
                matches("INSERT INTO schema_migration.*"),
                anyString(),
                anyString()
        );
    }

    @Test
    void assignOrphanRecords_shouldSkipWhenUserMissing() {
        when(jdbcTemplate.queryForObject(contains("schema_migration"), eq(Integer.class), any()))
                .thenReturn(0);
        when(jdbcTemplate.query(eq("PRAGMA table_info(saved_query)"), any(RowMapper.class)))
                .thenReturn(List.of("id", "name", "query"));
        when(jdbcTemplate.query(eq("PRAGMA table_info(note_template)"), any(RowMapper.class)))
                .thenReturn(List.of("id", "name", "content"));
        when(jdbcTemplate.query(eq("PRAGMA table_info(user_settings)"), any(RowMapper.class)))
                .thenReturn(List.of("id"));
        when(jdbcTemplate.query(
                eq("SELECT id FROM users WHERE email = ? LIMIT 1"),
                any(RowMapper.class),
                eq("darvik80@gmail.com")
        )).thenReturn(List.of());

        migrationService.runPendingMigrations();

        verify(jdbcTemplate, never()).update(
                matches("(?s).*UPDATE note_template SET owner_id = \\? WHERE owner_id IS NULL.*"),
                anyString()
        );
        verify(jdbcTemplate, never()).update(
                matches("(?s).*UPDATE saved_query SET owner_id = \\? WHERE owner_id IS NULL.*"),
                anyString()
        );
    }

    @Test
    void runPendingMigrations_shouldSkipAlreadyApplied() {
        when(jdbcTemplate.queryForObject(contains("schema_migration"), eq(Integer.class), any()))
                .thenReturn(1);

        migrationService.runPendingMigrations();

        verify(jdbcTemplate, never()).execute(matches("(?s).*ALTER TABLE saved_query ADD COLUMN owner_id.*"));
        verify(jdbcTemplate, never()).update(
                matches("INSERT INTO schema_migration.*"),
                anyString(),
                anyString()
        );
    }
}
