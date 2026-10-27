package dev.cogitor.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class VaultItemInput {
    private String id;
    @NotBlank
    private String title;
    private String type;
    private String folderId;
    private String username;
    private String host;
    private String url;
    private List<String> tags;
    private String password;
    private String privateKey;
    private String secret;
    private String extraJson;
}
