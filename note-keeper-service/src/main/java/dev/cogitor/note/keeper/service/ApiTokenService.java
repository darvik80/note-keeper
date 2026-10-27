package dev.cogitor.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.cogitor.note.keeper.dto.ApiTokenCreateRequest;
import dev.cogitor.note.keeper.dto.ApiTokenResponse;
import dev.cogitor.note.keeper.exception.ResourceNotFoundException;
import dev.cogitor.note.keeper.mapper.ApiTokenMapper;
import dev.cogitor.note.keeper.model.ApiToken;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTokenService {

    public static final String PREFIX = "nk_";
    public static final Set<String> ALLOWED_SCOPES = Set.of("notes", "todos", "vault", "mcp");

    private final ApiTokenMapper apiTokenMapper;
    private final SecureRandom random = new SecureRandom();

    public List<ApiTokenResponse> list(String userId) {
        return apiTokenMapper.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public ApiTokenResponse create(String userId, ApiTokenCreateRequest request) {
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String plaintext = PREFIX + secret;

        ApiToken token = new ApiToken();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setName(request.getName().trim());
        token.setTokenHash(hash(plaintext));
        token.setPrefix(plaintext.substring(0, Math.min(10, plaintext.length())));
        token.setScopes(normalizeScopes(request.getScopes()));
        token.setExpiresAt(parseExpires(request.getExpiresAt()));
        token.setCreatedAt(LocalDateTime.now());
        apiTokenMapper.insert(token);

        ApiTokenResponse response = toResponse(token);
        response.setToken(plaintext);
        log.info("Created API token {} for user {}", token.getId(), userId);
        return response;
    }

    public void revoke(String userId, String id) {
        ApiToken existing = apiTokenMapper.findById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new ResourceNotFoundException("Token not found");
        }
        apiTokenMapper.delete(id, userId);
        log.info("Revoked API token {} for user {}", id, userId);
    }

    public ApiToken authenticate(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(PREFIX)) {
            return null;
        }
        ApiToken token = apiTokenMapper.findByHash(hash(rawToken));
        if (token == null) {
            return null;
        }
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        try {
            apiTokenMapper.touchLastUsed(token.getId(), LocalDateTime.now());
        } catch (Exception e) {
            log.debug("Failed to touch last_used_at for token {}", token.getId());
        }
        return token;
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private List<String> normalizeScopes(List<String> scopes) {
        List<String> result = new ArrayList<>();
        if (scopes == null || scopes.isEmpty()) {
            return new ArrayList<>(ALLOWED_SCOPES);
        }
        for (String scope : scopes) {
            if (scope == null) {
                continue;
            }
            String normalized = scope.trim().toLowerCase(Locale.ROOT);
            if (ALLOWED_SCOPES.contains(normalized) && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("At least one valid scope is required");
        }
        return result;
    }

    private LocalDateTime parseExpires(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid expiresAt");
        }
    }

    private ApiTokenResponse toResponse(ApiToken token) {
        ApiTokenResponse response = new ApiTokenResponse();
        response.setId(token.getId());
        response.setName(token.getName());
        response.setPrefix(token.getPrefix() + "…");
        response.setScopes(token.getScopes());
        response.setExpiresAt(token.getExpiresAt());
        response.setLastUsedAt(token.getLastUsedAt());
        response.setCreatedAt(token.getCreatedAt());
        return response;
    }
}
