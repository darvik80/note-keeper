package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import dev.cogitor.note.keeper.dto.ApiTokenCreateRequest;
import dev.cogitor.note.keeper.dto.ApiTokenResponse;
import dev.cogitor.note.keeper.service.ApiTokenService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "API tokens")
@RequiredArgsConstructor
public class ApiTokenController {

    private final ApiTokenService apiTokenService;

    @GetMapping
    public List<ApiTokenResponse> list(@AuthenticationPrincipal String userId) {
        return apiTokenService.list(userId);
    }

    @PostMapping
    public ResponseEntity<ApiTokenResponse> create(@Valid @RequestBody ApiTokenCreateRequest request,
                                                   @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiTokenService.create(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable String id, @AuthenticationPrincipal String userId) {
        apiTokenService.revoke(userId, id);
        return ResponseEntity.noContent().build();
    }
}
