package dev.cogitor.note.keeper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import dev.cogitor.note.keeper.service.BackupService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduled backup configuration.
 * Automatically creates backups based on database settings.
 */
@Slf4j
@Component
public class BackupScheduler {

    @Autowired
    private BackupService backupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private PrometheusMetrics metrics;

    private ScheduledFuture<?> scheduledTask;
    private boolean initialized = false;
    private String lastCronExpr;
    private boolean lastEnabledState = false;

    /**
     * Check and update backup schedule based on database settings.
     * Called every 5 minutes to sync with database configuration.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void updateSchedule() {
        try {
            // Check if backup settings columns exist
            Boolean enabled = checkColumnExists("backup_auto_enabled")
                ? jdbcTemplate.queryForObject(
                    "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM user_settings WHERE backup_auto_enabled = 1",
                    Boolean.class
                )
                : false;

            String cron = checkColumnExists("backup_cron")
                ? jdbcTemplate.queryForObject(
                    "SELECT backup_cron FROM user_settings WHERE backup_auto_enabled = 1 LIMIT 1",
                    String.class
                )
                : "0 0 2 * * *";

            boolean isEnabled = enabled != null && enabled;
            String cronExpr = cron != null ? cron : "0 0 2 * * *";

            // Skip re-scheduling if nothing changed
            if (isEnabled == lastEnabledState && cronExpr.equals(lastCronExpr) && scheduledTask != null) {
                return;
            }

            // Cancel existing task if any
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                scheduledTask = null;
            }

            if (isEnabled) {
                log.info("Scheduling automatic backup with cron: {}", cronExpr);
                scheduledTask = taskScheduler.schedule(
                    this::executeBackup,
                    new CronTrigger(cronExpr)
                );
                lastCronExpr = cronExpr;
            } else {
                log.debug("Automatic backup is disabled or not configured");
            }
            lastEnabledState = isEnabled;
        } catch (Exception e) {
            log.warn("Backup settings not available yet: {}", e.getMessage());
        }
    }

    /**
     * Check if a column exists in user_settings table.
     */
    private boolean checkColumnExists(String columnName) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT " + columnName + " FROM user_settings LIMIT 1",
                Object.class
            );
            return true;
        } catch (Exception e) {
            // Column doesn't exist yet - this is normal during initial setup
            return false;
        }
    }

    /**
     * Execute the backup task.
     */
    private void executeBackup() {
        log.info("Starting scheduled automatic backup...");
        List<String> userIds;
        try {
            userIds = jdbcTemplate.queryForList(
                "SELECT id FROM user_settings WHERE backup_auto_enabled = 1",
                String.class
            );
        } catch (Exception e) {
            log.error("Scheduled backup failed: {}", e.getMessage(), e);
            return;
        }

        for (String userId : userIds) {
            long start = System.nanoTime();
            try {
                Path backupFile = backupService.exportData(userId);
                metrics.backupFinished(true, Duration.ofNanos(System.nanoTime() - start));
                log.info("Scheduled backup completed for user {}: {}", userId, backupFile.getFileName());
            } catch (IOException e) {
                metrics.backupFinished(false, Duration.ofNanos(System.nanoTime() - start));
                log.error("Scheduled backup failed for user {}: {}", userId, e.getMessage(), e);
            }
        }
    }

    /**
     * Initial backup schedule setup (runs once after startup).
     */
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 5000)
    public void init() {
        if (!initialized) {
            initialized = true;
            updateSchedule();
        }
    }
}
