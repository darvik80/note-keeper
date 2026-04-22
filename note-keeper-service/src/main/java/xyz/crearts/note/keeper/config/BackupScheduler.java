package xyz.crearts.note.keeper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import xyz.crearts.note.keeper.service.BackupService;

import java.io.IOException;
import java.nio.file.Path;
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

    private ScheduledFuture<?> scheduledTask;

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
                    "SELECT backup_auto_enabled FROM user_settings WHERE id = 'default'",
                    Boolean.class
                )
                : false;
            
            String cron = checkColumnExists("backup_cron")
                ? jdbcTemplate.queryForObject(
                    "SELECT backup_cron FROM user_settings WHERE id = 'default'",
                    String.class
                )
                : "0 0 2 * * *";

            boolean isEnabled = enabled != null && enabled;
            String cronExpr = cron != null ? cron : "0 0 2 * * *";

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
            } else {
                log.debug("Automatic backup is disabled or not configured");
            }
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
        try {
            Path backupFile = backupService.exportData();
            log.info("Scheduled backup completed: {}", backupFile.getFileName());
        } catch (IOException e) {
            log.error("Scheduled backup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Initial scheduled backup (runs once on startup if enabled).
     */
    @Scheduled(fixedDelay = 5000)
    public void init() {
        updateSchedule();
        // Unschedule this method after first execution
        // This is just to trigger initial schedule setup
    }
}
