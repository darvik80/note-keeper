package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import dev.cogitor.note.keeper.service.McpService;

@RestController
@RequestMapping("/mcp")
@Tag(name = "MCP")
@RequiredArgsConstructor
public class McpController {

    private final McpService mcpService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectNode handle(@RequestBody(required = false) JsonNode request,
                             @AuthenticationPrincipal String ownerId) {
        return mcpService.handle(request, ownerId);
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal String ownerId) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().name("endpoint").data("/mcp"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
