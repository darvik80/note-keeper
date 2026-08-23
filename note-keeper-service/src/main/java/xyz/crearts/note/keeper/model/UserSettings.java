package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSettings {
    private String id;
    private String telegramBotToken;
    private String telegramChatId;
    private String telegramWebhookSecret;
    private String dingtalkWebhook;
    private String dingtalkSecret;
    private boolean dailyReportEnabled;
    private String dailyReportTime;
    private String dailyReportChannels;
    private String dailyReportTemplate;
    private String dailyReportItemTemplate;
    private String dailyReportLastSent;
    private LocalDateTime updatedAt;
}
