package xyz.crearts.note.keeper.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public endpoint that exposes runtime configuration needed by the frontend.
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
            "googleClientId", googleClientId
        );
    }
}
