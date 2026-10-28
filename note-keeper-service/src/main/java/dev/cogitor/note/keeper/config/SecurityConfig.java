package dev.cogitor.note.keeper.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for JWT authentication with optional Google OAuth2.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired(required = false)
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${GOOGLE_CLIENT_ID:${app.oauth.google.client-id:}}")
    private String googleClientId;

    @Value("${YANDEX_CLIENT_ID:${app.oauth.yandex.client-id:}}")
    private String yandexClientId;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureCommon(http);

        boolean oauthEnabled = oAuth2SuccessHandler != null
                && (OAuthClients.enabled(googleClientId) || OAuthClients.enabled(yandexClientId));
        if (oauthEnabled) {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));
        } else {
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        }

        return http.build();
    }

    private void configureCommon(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/config").permitAll()
                .requestMatchers("/api/v1/telegram/webhook/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/", "/index.html", "/favicon.svg", "/favicon.ico",
                        "/static/**", "/assets/**", "/js/**",
                        "/*.js", "/*.css", "/*.png", "/*.jpg", "/*.svg").permitAll()
                .requestMatchers("/mcp", "/mcp/**").hasAuthority("SCOPE_mcp")
                .requestMatchers("/api/v1/vault/**").hasAuthority("SCOPE_vault")
                .requestMatchers("/api/v1/tokens/**").hasAuthority("SCOPE_tokens")
                .requestMatchers("/api/v1/notes/**", "/api/v1/templates/**", "/api/v1/tags/**").hasAuthority("SCOPE_notes")
                .requestMatchers("/api/v1/todos/**").hasAuthority("SCOPE_todos")
                .requestMatchers("/api/v1/search/**").hasAnyAuthority("SCOPE_notes", "SCOPE_todos")
                .requestMatchers("/api/v1/attachments/**").hasAnyAuthority("SCOPE_notes", "SCOPE_todos")
                .requestMatchers("/api/v1/settings/**", "/api/v1/backup/**", "/api/v1/analytics/**",
                        "/api/v1/integrations/**", "/api/v1/users/**").hasAuthority("SCOPE_settings")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/v1/**")
                )
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3001",
                "http://localhost:5173",
                "http://localhost:8080",
                "http://127.0.0.1:8080",
            "https://note.darvik.synology.me:8443"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*", "Authorization", "Content-Type"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
