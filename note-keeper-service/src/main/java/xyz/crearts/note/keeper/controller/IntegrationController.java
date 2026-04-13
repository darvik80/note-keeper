package xyz.crearts.note.keeper.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;
import xyz.crearts.note.keeper.service.IntegrationService;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping("/telegram")
    public IntegrationResponse sendToTelegram(@RequestBody IntegrationRequest request) {
        return integrationService.sendToTelegram(request);
    }

    @PostMapping("/dingtalk")
    public IntegrationResponse sendToDingTalk(@RequestBody IntegrationRequest request) {
        return integrationService.sendToDingTalk(request);
    }

    @PostMapping("/email")
    public IntegrationResponse sendEmail(@RequestBody IntegrationRequest request) {
        return integrationService.sendEmail(request);
    }
}
