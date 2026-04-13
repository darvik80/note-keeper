package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Attachment {
    private String id;
    private String parentId;
    private String parentType;
    private String name;
    private long size;
    private String type;
    private String url;
    private LocalDateTime uploadedAt;
}
