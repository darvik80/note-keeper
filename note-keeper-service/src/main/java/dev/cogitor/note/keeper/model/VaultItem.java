package dev.cogitor.note.keeper.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class VaultItem {
    private String id;
    private String folderId;
    private String ownerId;
    private String type;
    private String title;
    private String username;
    private String host;
    private String url;
    private List<String> tags = new ArrayList<>();
    @JsonIgnore
    private String secretBlob;
    private Integer revision;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private String password;
    private String privateKey;
    private String secret;
    private String extraJson;
}
