package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.dto.AnalyticsResponse;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private NoteMapper noteMapper;
    @Mock private TodoMapper todoMapper;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(noteMapper, todoMapper);
    }

    @Test
    void getAnalytics_week_shouldReturn7DaysActivity() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(5);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(3);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(2);
        when(noteMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(1);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);

        AnalyticsResponse response = analyticsService.getAnalytics("week", "owner-1");

        assertNotNull(response);
        assertEquals(5, response.getNotesCreated());
        assertEquals(3, response.getTodosCreated());
        assertEquals(2, response.getTodosCompleted());
        assertEquals(7, response.getDailyActivity().size());
        assertNotNull(response.getPriorityDistribution());
        assertEquals(3, response.getPriorityDistribution().size());
    }

    @Test
    void getAnalytics_month_shouldReturn30DaysActivity() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(10);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(8);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(4);
        when(noteMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);

        AnalyticsResponse response = analyticsService.getAnalytics("month", "owner-1");

        assertEquals(30, response.getDailyActivity().size());
    }

    @Test
    void getAnalytics_year_shouldReturn365DaysActivity() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(100);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(50);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(25);
        when(noteMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);

        AnalyticsResponse response = analyticsService.getAnalytics("year", "owner-1");

        assertEquals(365, response.getDailyActivity().size());
    }

    @Test
    void getAnalytics_nullTimeRange_shouldDefaultToWeek() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(noteMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);

        AnalyticsResponse response = analyticsService.getAnalytics(null, "owner-1");

        assertEquals(7, response.getDailyActivity().size());
    }

    @Test
    void getAnalytics_completionRate_shouldCalculateCorrectly() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(10);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(5);
        when(noteMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);

        AnalyticsResponse response = analyticsService.getAnalytics("week", "owner-1");

        assertEquals(50.0, response.getCompletionRate());
    }

    @Test
    void getAnalytics_zeroTodos_completionRateShouldBeZero() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(noteMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(0);

        AnalyticsResponse response = analyticsService.getAnalytics("week", "owner-1");

        assertEquals(0.0, response.getCompletionRate());
    }

    @Test
    void getAnalytics_shouldIncludePriorityDistribution() {
        when(noteMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(todoMapper.countCompletedByDateRange(anyString(), anyString(), eq("owner-1"))).thenReturn(0);
        when(noteMapper.countByPriority(eq("high"), anyString(), anyString(), eq("owner-1"))).thenReturn(3);
        when(noteMapper.countByPriority(eq("medium"), anyString(), anyString(), eq("owner-1"))).thenReturn(5);
        when(noteMapper.countByPriority(eq("low"), anyString(), anyString(), eq("owner-1"))).thenReturn(2);
        when(todoMapper.countByPriority(anyString(), anyString(), anyString(), eq("owner-1"))).thenReturn(1);

        AnalyticsResponse response = analyticsService.getAnalytics("week", "owner-1");

        assertEquals(4, response.getPriorityDistribution().get("high"));   // 3 notes + 1 todo
        assertEquals(6, response.getPriorityDistribution().get("medium")); // 5 notes + 1 todo
        assertEquals(3, response.getPriorityDistribution().get("low"));    // 2 notes + 1 todo
    }
}
