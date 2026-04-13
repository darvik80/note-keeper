package xyz.crearts.note.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
public class AnalyticsResponse {
    private int notesCreated;
    private int todosCreated;
    private int todosCompleted;
    private double completionRate;
    private List<TagCount> topTags;
    private Map<String, Integer> priorityDistribution;
    private List<Integer> dailyActivity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagCount {
        private String tag;
        private int count;
    }
}
