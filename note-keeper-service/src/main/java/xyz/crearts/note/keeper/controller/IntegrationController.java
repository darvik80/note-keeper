package xyz.crearts.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;
import xyz.crearts.note.keeper.service.IntegrationService;

@RestController
@RequestMapping("/api/v1/integrations")
@Tag(name = "Integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @PostMapping("/telegram")
    public IntegrationResponse sendToTelegram(@RequestBody IntegrationRequest request) {
        return integrationService.sendToTelegram(request);
    }

    @PostMapping("/dingtalk")
    public IntegrationResponse sendToDingTalk(@RequestBody IntegrationRequest request) {
        return integrationService.sendToDingTalk(request);
    }

    @PostMapping("/telegram/test-todo")
    public IntegrationResponse sendTestTodoToTelegram(@AuthenticationPrincipal String userId) {
        return integrationService.sendTestTodoToTelegram(userId);
    }

}
