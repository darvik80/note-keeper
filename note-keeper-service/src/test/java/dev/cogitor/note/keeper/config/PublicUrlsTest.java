package dev.cogitor.note.keeper.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicUrlsTest {

    @Test
    void prependsHttpsWhenSchemeMissing() {
        assertEquals("https://note.cogitor.dev", PublicUrls.normalize("note.cogitor.dev"));
        assertEquals("https://note.cogitor.dev", PublicUrls.normalize("note.cogitor.dev/"));
    }

    @Test
    void keepsExplicitScheme() {
        assertEquals("https://note.cogitor.dev", PublicUrls.normalize("https://note.cogitor.dev/"));
        assertEquals("http://localhost:8080", PublicUrls.normalize("http://localhost:8080"));
    }

    @Test
    void emptyStaysEmpty() {
        assertEquals("", PublicUrls.normalize(null));
        assertEquals("", PublicUrls.normalize("  "));
    }
}
