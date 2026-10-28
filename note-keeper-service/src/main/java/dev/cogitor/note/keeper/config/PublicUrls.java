package dev.cogitor.note.keeper.config;

/**
 * Public origin for OAuth redirects and links in reports.
 * Debconf often gets a bare host ({@code note.example.com}); {@code sendRedirect}
 * then treats it as a relative path.
 */
public final class PublicUrls {

    private PublicUrls() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String url = raw.trim().replaceAll("/+$", "");
        if (url.isEmpty()) {
            return "";
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return "https://" + url;
        }
        return url;
    }
}
