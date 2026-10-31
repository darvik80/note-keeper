package dev.cogitor.note.keeper.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * App gauges and business counters next to the JVM/HTTP metrics Actuator already exports.
 */
@Component
public class PrometheusMetrics {

    private final MeterRegistry registry;
    private final Counter notesCreated;
    private final Counter notesArchived;
    private final Counter backupsSucceeded;
    private final Counter backupsFailed;
    private final Timer backupDuration;

    public PrometheusMetrics(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        this.registry = registry;
        // Prometheus reserves the "_created" suffix (OpenMetrics), so "notes.created" would
        // be exposed as notekeeper_notes_total and collide with the notes gauge below.
        this.notesCreated = Counter.builder("notekeeper.notes.added")
                .description("Notes created")
                .register(registry);
        this.notesArchived = Counter.builder("notekeeper.notes.archived")
                .description("Notes archived")
                .register(registry);
        this.backupsSucceeded = Counter.builder("notekeeper.backups")
                .description("Scheduled backups by result")
                .tag("result", "success")
                .register(registry);
        this.backupsFailed = Counter.builder("notekeeper.backups")
                .description("Scheduled backups by result")
                .tag("result", "failure")
                .register(registry);
        this.backupDuration = Timer.builder("notekeeper.backup.duration")
                .description("Scheduled backup duration per user")
                .publishPercentileHistogram()
                .register(registry);

        // "notekeeper.records." keeps these gauges clear of the notes.* business counters
        bindCount(registry, jdbcTemplate, "notekeeper.records.users", "users", "Registered user accounts");
        bindCount(registry, jdbcTemplate, "notekeeper.records.notes", "note", "Notes including archived and trash");
        bindCount(registry, jdbcTemplate, "notekeeper.records.todos", "todo", "Todos including archived and trash");
        bindCount(registry, jdbcTemplate, "notekeeper.records.vault_items", "vault_item", "Vault items");
    }

    public void noteCreated() {
        notesCreated.increment();
    }

    public void noteArchived() {
        notesArchived.increment();
    }

    public void login(String provider, boolean success) {
        registry.counter("notekeeper.auth.logins",
                "provider", provider,
                "result", success ? "success" : "failure").increment();
    }

    public void authRejected(String reason) {
        registry.counter("notekeeper.auth.rejections", "reason", reason).increment();
    }

    public void telegramWebhookError(String reason) {
        registry.counter("notekeeper.telegram.webhook.errors", "reason", reason).increment();
    }

    public void backupFinished(boolean success, Duration duration) {
        backupDuration.record(duration);
        (success ? backupsSucceeded : backupsFailed).increment();
    }

    public void reminderSent(String channel, String result) {
        registry.counter("notekeeper.reminders.sent",
                "channel", channel,
                "result", result).increment();
    }

    private static void bindCount(MeterRegistry registry, JdbcTemplate jdbc, String name, String table, String description) {
        Gauge.builder(name, jdbc, j -> count(j, table))
                .description(description)
                .register(registry);
    }

    private static double count(JdbcTemplate jdbc, String table) {
        try {
            Number n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Number.class);
            return n == null ? 0 : n.doubleValue();
        } catch (Exception ignored) {
            return 0;
        }
    }
}
