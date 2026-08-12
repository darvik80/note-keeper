package xyz.crearts.note.keeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
    @Getter(onMethod_ = @JsonProperty("isFavorite"))
    @Setter(onMethod_ = @JsonProperty("isFavorite"))
    private boolean favorite;
    @Getter(onMethod_ = @JsonProperty("isEncrypted"))
    @Setter(onMethod_ = @JsonProperty("isEncrypted"))
    private boolean encrypted;
    @Getter(onMethod_ = @JsonProperty("isArchived"))
    @Setter(onMethod_ = @JsonProperty("isArchived"))
    private boolean archived;
    @Getter(onMethod_ = @JsonProperty("isDeleted"))
    @Setter(onMethod_ = @JsonProperty("isDeleted"))
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime reminder;
    private String templateId;
    private String ownerId;
    private String sharedWith; // JSON array of user IDs
    private List<Attachment> attachments = new ArrayList<>();
    private List<NoteHistory> history = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
