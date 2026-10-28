package dev.cogitor.note.keeper.config;

import org.springframework.util.StringUtils;

/**
 * Google and Yandex are independently optional. Empty or native AOT dummy IDs stay off.
 */
public final class OAuthClients {

    private OAuthClients() {
    }

    public static boolean enabled(String clientId) {
        return StringUtils.hasText(clientId) && !clientId.startsWith("aot-dummy");
    }
}
