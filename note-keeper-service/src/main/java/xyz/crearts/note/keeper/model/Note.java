package xyz.crearts.note.keeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Note {
    private String id;
    private String title;
    private String content;
    private List<String> tags = new ArrayList<>();
    private String folder;
    private String subfolder;
    private String priority;
    @JsonProperty("isFavorite")
    private boolean favorite;
    @JsonProperty("isEncrypted")
    private boolean encrypted;
    @JsonProperty("isArchived")
    private boolean archived;
    @JsonProperty("isDeleted")
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime reminder;
    private String templateId;
    private List<Attachment> attachments = new ArrayList<>();
    private List<NoteHistory> history = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
