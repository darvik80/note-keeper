package dev.cogitor.note.keeper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Builds Google/Yandex registrations only when that provider's client-id is set.
 * Avoids Boot's {@code OAuth2ClientProperties} validation, which rejects empty client-id
 * and would otherwise make one provider require the other.
 */
@Configuration
public class OptionalOAuth2ClientConfiguration {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID:${app.oauth.google.client-id:}}") String googleClientId,
            @Value("${GOOGLE_CLIENT_SECRET:${app.oauth.google.client-secret:}}") String googleClientSecret,
            @Value("${GOOGLE_REDIRECT_URI:${app.oauth.google.redirect-uri:}}") String googleRedirect,
            @Value("${YANDEX_CLIENT_ID:${app.oauth.yandex.client-id:}}") String yandexClientId,
            @Value("${YANDEX_CLIENT_SECRET:${app.oauth.yandex.client-secret:}}") String yandexClientSecret,
            @Value("${YANDEX_REDIRECT_URI:${app.oauth.yandex.redirect-uri:}}") String yandexRedirect) {
        List<ClientRegistration> registrations = new ArrayList<>();
        if (OAuthClients.enabled(googleClientId)) {
            registrations.add(google(googleClientId, googleClientSecret, googleRedirect));
        }
        if (OAuthClients.enabled(yandexClientId)) {
            registrations.add(yandex(yandexClientId, yandexClientSecret, yandexRedirect));
        }
        if (registrations.isEmpty()) {
            return new EmptyClientRegistrationRepository();
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    private static ClientRegistration google(String clientId, String clientSecret, String redirect) {
        return ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(StringUtils.hasText(redirect) ? redirect : "{baseUrl}/login/oauth2/code/google")
                .scope("profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v3/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .clientName("Google")
                .build();
    }

    private static ClientRegistration yandex(String clientId, String clientSecret, String redirect) {
        return ClientRegistration.withRegistrationId("yandex")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(StringUtils.hasText(redirect) ? redirect : "{baseUrl}/login/oauth2/code/yandex")
                .scope("login:email", "login:info", "login:avatar")
                .authorizationUri("https://oauth.yandex.ru/authorize")
                .tokenUri("https://oauth.yandex.ru/token")
                .userInfoUri("https://login.yandex.ru/info?format=json")
                .userNameAttributeName("id")
                .clientName("Yandex")
                .build();
    }

    /**
     * {@link InMemoryClientRegistrationRepository} forbids an empty list.
     */
    static final class EmptyClientRegistrationRepository
            implements ClientRegistrationRepository, Iterable<ClientRegistration> {

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }
}
