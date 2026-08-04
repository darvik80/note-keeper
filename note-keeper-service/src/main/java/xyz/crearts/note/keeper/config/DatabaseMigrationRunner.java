package xyz.crearts.note.keeper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final DatabaseMigrationService migrationService;

    public DatabaseMigrationRunner(DatabaseMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking database migrations...");
        migrationService.runPendingMigrations();
    }
}
