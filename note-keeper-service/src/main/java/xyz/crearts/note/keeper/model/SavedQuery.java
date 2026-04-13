package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SavedQuery {
    private String id;
    private String name;
    private String query;
    private Filters filters;
    private LocalDateTime createdAt;

    @Data
    public static class Filters {
        private String type;
        private List<String> tags = new ArrayList<>();
        private String priority;
        private String folder;
    }
}
