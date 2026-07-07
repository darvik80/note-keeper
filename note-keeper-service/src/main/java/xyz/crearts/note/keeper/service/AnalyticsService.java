package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.AnalyticsResponse;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final NoteMapper noteMapper;
    private final TodoMapper todoMapper;

    public AnalyticsService(NoteMapper noteMapper, TodoMapper todoMapper) {
        this.noteMapper = noteMapper;
        this.todoMapper = todoMapper;
    }

    public AnalyticsResponse getAnalytics(String timeRange, String ownerId) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        int days;

        switch (timeRange != null ? timeRange : "week") {
            case "month" -> { startDate = now.minusMonths(1); days = 30; }
            case "year" -> { startDate = now.minusYears(1); days = 365; }
            default -> { startDate = now.minusWeeks(1); days = 7; }
        }

        String start = startDate.atStartOfDay().toString();
        String end = now.atTime(LocalTime.MAX).toString();

        int notesCreated = noteMapper.countByDateRange(start, end, ownerId);
        int todosCreated = todoMapper.countByDateRange(start, end, ownerId);
        int todosCompleted = todoMapper.countCompletedByDateRange(start, end, ownerId);
        double completionRate = todosCreated > 0 ? (double) todosCompleted / todosCreated * 100.0 : 0.0;

        Map<String, Integer> priorityDistribution = new HashMap<>();
        for (String p : List.of("high", "medium", "low")) {
            int noteCount = noteMapper.countByPriority(p, start, end, ownerId);
            int todoCount = todoMapper.countByPriority(p, start, end, ownerId);
            priorityDistribution.put(p, noteCount + todoCount);
        }

        List<Integer> dailyActivity = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = now.minusDays(i);
            String dayStart = day.atStartOfDay().toString();
            String dayEnd = day.atTime(LocalTime.MAX).toString();
            int noteCount = noteMapper.countByDateRange(dayStart, dayEnd, ownerId);
            int todoCount = todoMapper.countByDateRange(dayStart, dayEnd, ownerId);
            dailyActivity.add(noteCount + todoCount);
        }

        AnalyticsResponse response = new AnalyticsResponse();
        response.setNotesCreated(notesCreated);
        response.setTodosCreated(todosCreated);
        response.setTodosCompleted(todosCompleted);
        response.setCompletionRate(Math.round(completionRate * 10.0) / 10.0);
        response.setTopTags(new ArrayList<>());
        response.setPriorityDistribution(priorityDistribution);
        response.setDailyActivity(dailyActivity);

        return response;
    }
}
