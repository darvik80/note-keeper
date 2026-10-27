package dev.cogitor.note.keeper.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import dev.cogitor.note.keeper.model.ApiToken;
import dev.cogitor.note.keeper.service.ApiTokenService;
import dev.cogitor.note.keeper.service.JwtService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT / PAT authentication filter.
 * Bearer nk_… → personal access token; otherwise JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SESSION_SCOPES = List.of(
            "notes", "todos", "vault", "mcp", "tokens", "settings"
    );

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final ApiTokenService apiTokenService;

    public JwtAuthenticationFilter(JwtService jwtService, ApiTokenService apiTokenService) {
        this.jwtService = jwtService;
        this.apiTokenService = apiTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (token.startsWith(ApiTokenService.PREFIX)) {
            authenticatePat(request, token);
        } else {
            authenticateJwt(request, token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateJwt(HttpServletRequest request, String token) {
        String userId = jwtService.validateToken(token);
        if (userId == null) {
            return;
        }
        setAuthentication(request, userId, false, null, SESSION_SCOPES);
        log.debug("Set JWT authentication for user: {}", userId);
    }

    private void authenticatePat(HttpServletRequest request, String token) {
        ApiToken apiToken = apiTokenService.authenticate(token);
        if (apiToken == null) {
            return;
        }
        List<String> scopes = apiToken.getScopes() == null ? List.of() : apiToken.getScopes();
        setAuthentication(request, apiToken.getUserId(), true, apiToken.getId(), scopes);
        log.debug("Set PAT authentication for user: {}", apiToken.getUserId());
    }

    private void setAuthentication(HttpServletRequest request, String userId, boolean pat,
                                   String tokenId, List<String> scopes) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String scope : scopes) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        request.setAttribute("apiAuthDetails", new ApiAuthDetails(pat, tokenId, List.copyOf(scopes)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
