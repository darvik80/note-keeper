package dev.cogitor.note.keeper.config;

import java.util.List;

/**
 * Authentication details stored on the SecurityContext.
 * JWT sessions have all scopes; PAT sessions have the token's scopes.
 */
public record ApiAuthDetails(boolean pat, String tokenId, List<String> scopes) {
}
