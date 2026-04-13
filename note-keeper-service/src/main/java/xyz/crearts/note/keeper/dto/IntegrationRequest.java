package xyz.crearts.note.keeper.dto;

import lombok.Data;

@Data
public class IntegrationRequest {
    private String message;
    private String subject;
    private String botToken;
    private String chatId;
    private String webhook;
    private String secret;
    private String to;
}
