package xyz.crearts.note.keeper.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Central OpenAPI / Swagger UI configuration.
 * Uses springdoc-openapi v3 (Spring Boot 4 compatible).
 *
 * Swagger UI:  /swagger-ui/index.html
 * OpenAPI JSON: /v3/api-docs
 * OpenAPI YAML: /v3/api-docs.yaml
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "NoteKeeper API",
                version = "3.0.0",
                description = """
                        Open-source Evernote alternative with powerful features for notes and todos management.

                        ## Features
                        - **Notes** — CRUD, Markdown & Mermaid, encryption, version history
                        - **Todos** — descriptions, tags, priorities, schedules, reminders
                        - **Folders & Tags** — hierarchical organisation
                        - **Search** — full-text search with saved queries
                        - **Integrations** — Telegram, DingTalk, Email notifications
                        - **Daily Report** — scheduled summary of pending todos
                        - **Backup** — export / import, scheduled automatic backups
                        """,
                contact = @Contact(name = "NoteKeeper Support"),
                license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
        ),
        servers = {
                @Server(url = "/", description = "Current server")
        },
        tags = {
                @Tag(name = "Auth", description = "Authentication & registration"),
                @Tag(name = "Notes", description = "Notes CRUD, lifecycle, sharing"),
                @Tag(name = "Todos", description = "Todos CRUD, lifecycle, sharing"),
                @Tag(name = "Templates", description = "Note templates"),
                @Tag(name = "Tags", description = "Tag listing"),
                @Tag(name = "Search", description = "Full-text search & saved queries"),
                @Tag(name = "Analytics", description = "Productivity statistics"),
                @Tag(name = "Integrations", description = "Telegram, DingTalk, Email notifications"),
                @Tag(name = "Settings", description = "User settings & daily report"),
                @Tag(name = "Attachments", description = "File attachments"),
                @Tag(name = "Backup", description = "Data export / import"),
                @Tag(name = "Users", description = "User profile"),
                @Tag(name = "Config", description = "Public configuration"),
                @Tag(name = "Telegram Webhook", description = "Telegram callback receiver (internal)")
        }
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter the JWT token obtained from /api/v1/auth/login or /api/v1/auth/register"
)
public class OpenApiConfig {
}
