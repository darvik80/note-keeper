package xyz.crearts.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.crearts.note.keeper.model.Attachment;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteInput {
    @NotBlank(message = "Title is required")
    private String title;
    private String content;
    private List<String> tags;
    private String folder;
    private String subfolder;
    private String priority;
    private Boolean isFavorite;
    private Boolean isEncrypted;
    private LocalDateTime reminder;
    private List<Attachment> attachments;
    private String templateId;
}
