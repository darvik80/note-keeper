package xyz.crearts.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.crearts.note.keeper.dto.AnalyticsResponse;
import xyz.crearts.note.keeper.service.AnalyticsService;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsResponse getAnalytics(
            @RequestParam(required = false, defaultValue = "week") String timeRange,
            @AuthenticationPrincipal String ownerId) {
        return analyticsService.getAnalytics(timeRange, ownerId);
    }
}
