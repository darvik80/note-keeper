package xyz.crearts.note.keeper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private String baseDir;
    private String dataDir;
    private String attachmentsDir;
    private String backupsDir;
}
