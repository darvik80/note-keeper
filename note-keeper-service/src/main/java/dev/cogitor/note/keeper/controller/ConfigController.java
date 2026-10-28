package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import dev.cogitor.note.keeper.config.OAuthClients;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public endpoint that exposes runtime configuration needed by clients.
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Config")
public class ConfigController {

    private final String googleClientId;
    private final String yandexClientId;

    public ConfigController(
            @Value("${GOOGLE_CLIENT_ID:${app.oauth.google.client-id:}}") String googleClientId,
            @Value("${YANDEX_CLIENT_ID:${app.oauth.yandex.client-id:}}") String yandexClientId) {
        this.googleClientId = googleClientId;
        this.yandexClientId = yandexClientId;
    }

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
                "status", "ok",
                "oauth", Map.of(
                        "google", OAuthClients.enabled(googleClientId),
                        "yandex", OAuthClients.enabled(yandexClientId)
                )
        );
    }
}
