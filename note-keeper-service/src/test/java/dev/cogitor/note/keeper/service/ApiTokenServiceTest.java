package dev.cogitor.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.cogitor.note.keeper.dto.ApiTokenCreateRequest;
import dev.cogitor.note.keeper.dto.ApiTokenResponse;
import dev.cogitor.note.keeper.mapper.ApiTokenMapper;
import dev.cogitor.note.keeper.model.ApiToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    @Mock private ApiTokenMapper apiTokenMapper;
    private ApiTokenService service;

    @BeforeEach
    void setUp() {
        service = new ApiTokenService(apiTokenMapper);
    }

    @Test
    void create_returnsPlaintextOnceAndStoresHash() {
        ApiTokenCreateRequest request = new ApiTokenCreateRequest();
        request.setName("FAR");
        request.setScopes(List.of("vault", "mcp"));

        ApiTokenResponse created = service.create("user-1", request);

        assertTrue(created.getToken().startsWith("nk_"));
        assertEquals("FAR", created.getName());
        assertEquals(List.of("vault", "mcp"), created.getScopes());

        ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
        verify(apiTokenMapper).insert(captor.capture());
        ApiToken stored = captor.getValue();
        assertEquals(ApiTokenService.hash(created.getToken()), stored.getTokenHash());
        assertNull(stored.getExpiresAt());
        assertFalse(stored.getTokenHash().contains("nk_"));
    }

    @Test
    void authenticate_acceptsValidToken() {
        ApiTokenCreateRequest request = new ApiTokenCreateRequest();
        request.setName("MCP");
        request.setScopes(List.of("mcp", "notes"));
        ApiTokenResponse created = service.create("user-1", request);

        ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
        verify(apiTokenMapper).insert(captor.capture());
        when(apiTokenMapper.findByHash(ApiTokenService.hash(created.getToken()))).thenReturn(captor.getValue());

        ApiToken authed = service.authenticate(created.getToken());
        assertNotNull(authed);
        assertEquals("user-1", authed.getUserId());
        verify(apiTokenMapper).touchLastUsed(eq(authed.getId()), any());
    }

    @Test
    void authenticate_rejectsUnknownToken() {
        assertNull(service.authenticate("nk_not-a-real-token"));
        assertNull(service.authenticate("eyJhbGciOiJIUzI1NiJ9"));
    }
}
