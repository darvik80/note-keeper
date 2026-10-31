package dev.cogitor.note.keeper.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * App gauges next to the JVM/HTTP metrics Actuator already exports.
 */
@Component
public class PrometheusMetrics {

    public PrometheusMetrics(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        bindCount(registry, jdbcTemplate, "notekeeper.users", "users", "Registered user accounts");
        bindCount(registry, jdbcTemplate, "notekeeper.notes", "note", "Notes including archived and trash");
        bindCount(registry, jdbcTemplate, "notekeeper.todos", "todo", "Todos including archived and trash");
        bindCount(registry, jdbcTemplate, "notekeeper.vault.items", "vault_item", "Vault items");
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
