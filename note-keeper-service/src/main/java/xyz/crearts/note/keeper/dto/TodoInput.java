package xyz.crearts.note.keeper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TodoInput {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    private Boolean completed;
    private List<String> tags;
    private String priority;
    private Boolean isFavorite;
    
    private String dueDate;
    private String reminder;
    private String notificationChannels;
    
    private Map<String, Object> location;
    private Map<String, Object> schedule;
    private List<Map<String, Object>> attachments;
}
