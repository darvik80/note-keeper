package xyz.crearts.note.keeper.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes storage directories before database connection.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class StorageInitializer {
    private final StorageProperties props;
    @PostConstruct
    public void init() throws Exception {
        Path basePath = Path.of(props.getBaseDir());
        Files.createDirectories(basePath);
        Files.createDirectories(Path.of(props.getDataDir()));
        Files.createDirectories(Path.of(props.getAttachmentsDir()));
        Files.createDirectories(Path.of(props.getBackupsDir()));
        log.info("Storage directories initialized: {}", basePath.toAbsolutePath());
    }
}
