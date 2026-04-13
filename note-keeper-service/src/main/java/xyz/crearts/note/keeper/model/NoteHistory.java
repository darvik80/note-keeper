package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteHistory {
    private String id;
    private String noteId;
    private String content;
    private LocalDateTime timestamp;
    private String action;
}
