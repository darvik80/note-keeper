package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSettings {
    private String id;
    private String telegramBotToken;
    private String telegramChatId;
    private String dingtalkWebhook;
    private String dingtalkSecret;
    private LocalDateTime updatedAt;
}
