package dev.cogitor.note.keeper.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthCodeServiceTest {

    @Test
    void consume_once_returnsJwt() {
        OAuthCodeService service = new OAuthCodeService();
        String code = service.issue("jwt-1", "state-a");
        assertEquals("jwt-1", service.consume(code, "state-a"));
        assertNull(service.consume(code, "state-a"));
    }

    @Test
    void consume_wrongState_keepsCode() {
        OAuthCodeService service = new OAuthCodeService();
        String code = service.issue("jwt-1", "state-a");
        assertNull(service.consume(code, "attacker"));
        assertEquals("jwt-1", service.consume(code, "state-a"));
    }

    @Test
    void consume_unknown_returnsNull() {
        assertNull(new OAuthCodeService().consume("nope", "state"));
    }
}
