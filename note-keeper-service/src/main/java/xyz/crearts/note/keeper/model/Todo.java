package xyz.crearts.note.keeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Todo {
    private String id;
    private String title;
    private String description;
    private boolean completed;
    private List<String> tags = new ArrayList<>();
    private String priority;
    @Getter(onMethod_ = @JsonProperty("isFavorite"))
    @Setter(onMethod_ = @JsonProperty("isFavorite"))
    private boolean favorite;
    @Getter(onMethod_ = @JsonProperty("isArchived"))
    @Setter(onMethod_ = @JsonProperty("isArchived"))
    private boolean archived;
    @Getter(onMethod_ = @JsonProperty("isDeleted"))
    @Setter(onMethod_ = @JsonProperty("isDeleted"))
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime dueDate;
    private LocalDateTime reminder;
    private LocalDateTime notifiedAt;
    private String notificationChannels; // Comma-separated: telegram,dingtalk
    private String ownerId;
    private String sharedWith; // JSON array of user IDs
    private Schedule schedule;
    private List<Attachment> attachments = new ArrayList<>();
    private LocalDateTime lastCompletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class Schedule {
        private String repeat;
        private LocalDateTime endDate;
        /** Day-of-week indices for custom/weekdays (0=Sun … 6=Sat, JS Date.getDay). */
        private List<Integer> daysOfWeek;
    }
}
