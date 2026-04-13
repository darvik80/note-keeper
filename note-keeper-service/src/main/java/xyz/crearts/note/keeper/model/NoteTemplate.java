package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class NoteTemplate {
    private String id;
    private String name;
    private String content;
    private List<String> tags = new ArrayList<>();
    private String category;
    private LocalDateTime createdAt;
}
