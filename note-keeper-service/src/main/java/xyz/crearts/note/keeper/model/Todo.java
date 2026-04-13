package xyz.crearts.note.keeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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
    @JsonProperty("isFavorite")
    private boolean favorite;
    @JsonProperty("isArchived")
    private boolean archived;
    @JsonProperty("isDeleted")
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime dueDate;
    private LocalDateTime reminder;
    private Location location;
    private Schedule schedule;
    private List<Attachment> attachments = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class Location {
        private Double lat;
        private Double lng;
        private String address;
    }

    @Data
    public static class Schedule {
        private String repeat;
        private LocalDateTime endDate;
    }
}
