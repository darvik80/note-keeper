package dev.cogitor.note.keeper.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time OAuth exchange codes. JWT never travels in a query string.
 */
@Service
public class OAuthCodeService {

    static final Duration TTL = Duration.ofSeconds(60);

    private final ConcurrentHashMap<String, Entry> codes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String jwt, String state) {
        purgeExpired();
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        codes.put(code, new Entry(jwt, state, Instant.now().plus(TTL)));
        return code;
    }

    public String consume(String code, String state) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Entry entry = codes.get(code);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expires())) {
            codes.remove(code);
            return null;
        }
        if (entry.state() != null && !entry.state().equals(state)) {
            return null;
        }
        if (!codes.remove(code, entry)) {
            return null;
        }
        return entry.jwt();
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(e -> now.isAfter(e.getValue().expires()));
    }

    private record Entry(String jwt, String state, Instant expires) {
        Entry {
            Objects.requireNonNull(jwt);
        }
    }
}
