package dev.cogitor.note.keeper.config;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrometheusMetricsTest {

    private PrometheusMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    void notesCounterAndRecordGauges_shouldNotCollide() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Number.class))).thenReturn(3);
        new PrometheusMetrics(registry, jdbc);

        String scrape = registry.scrape();

        // "notekeeper.notes.created" would be exported as notekeeper_notes_total and drop the gauge
        assertTrue(scrape.contains("notekeeper_notes_added_total"));
        assertTrue(scrape.contains("notekeeper_notes_archived_total"));
        assertTrue(scrape.contains("notekeeper_records_notes"));
        assertTrue(scrape.contains("notekeeper_records_users"));
    }
}
