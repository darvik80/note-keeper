package xyz.crearts.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public endpoint that exposes runtime configuration needed by the frontend.
 */
@RestController
@RequestMapping("/api/v1/config")
@Tag(name = "Config")
public class ConfigController {

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of("status", "ok");
    }
}
