# NoteKeeper

Open-source Evernote alternative with powerful features for notes and todos management. Self-hosted, single-binary deployment with GraalVM native image support.

## Features

### Core
- 📝 **Notes Management** — Create, edit, organize notes with full Markdown support, version history, and import from text files
- ✅ **Todo Management** — Task tracking with priorities, due dates, reminders, recurrence (daily/weekly/monthly/weekdays/custom), and completion logging
- 📁 **Folder Organization** — Hierarchical folder/subfolder structure with tree navigation
- 🔍 **Full-text Search** — Search across all notes and todos with savable queries and filters
- 📅 **Calendar View** — Visual calendar for tasks with due dates and reminders
- 📊 **Analytics Dashboard** — Notes/todos created, completion rate, top tags, priority distribution, daily activity chart

### Organization
- ⭐ **Favorites** — Mark important notes and todos
- 📦 **Archive** — Archive completed items without deleting
- 🗑️ **Trash** — Soft delete with recovery; permanent delete option
- 📋 **Templates** — Reusable Markdown templates with categories for quick note creation
- 🏷️ **Tags** — User-scoped tag management with auto-suggestions

### Security & Privacy
- 🔐 **AES-256-GCM Encryption** — Encrypt sensitive notes server-side
- 🛡️ **JWT Authentication** — Stateless token-based auth (HS256, 24h expiry)
- 🔑 **Google OAuth2** — Social login via Google
- 🔒 **Password Security** — SHA-256 + random salt hashing

### Collaboration & Notifications
- 👥 **Sharing** — Share notes and todos with other users; "Shared with me" views
- 🔔 **Telegram Notifications** — Bot-based reminders with MarkdownV2 support and webhook callbacks
- 💬 **DingTalk Notifications** — Webhook-based reminders with HMAC-SHA256 signing
- 📰 **Daily Reports** — Scheduled summary of pending todos via Telegram/DingTalk with customizable templates
- 📡 **Per-todo Notification Channels** — Choose telegram, dingtalk, or both for each reminder

### Files & Data
- 📎 **Attachments** — File attachments for notes and todos (single + batch upload)
- 🔄 **Backup & Restore** — Manual and scheduled automatic backups with configurable retention; download, list, delete
- 📜 **Note History** — Version tracking (created/edited/restored) with restore capability
- 🔌 **WebSocket Notifications** — Real-time push updates to connected clients

### UI/UX
- 🎨 **12 Themes** — Light, Dark, Green, Cyan, Blue, Purple, Darcula, Rose, Amber, Teal, Indigo, Slate
- 📱 **Responsive Design** — Desktop sidebar layout + mobile drawer with hamburger menu
- ⌨️ **Keyboard Shortcuts** — Customizable hotkeys (new note, new todo, search, toggle sidebar)
- 🍞 **Toast Notifications** — Contextual feedback for user actions
- ⚡ **Lazy Loading** — Code-split pages with Suspense fallbacks

## Tech Stack

### Backend
| Component        | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 25                             |
| Framework        | Spring Boot 4.1                     |
| ORM              | MyBatis 4.1 (XML mappers)           |
| Database         | SQLite (default) / PostgreSQL       |
| Auth             | Spring Security + JWT (JJWT 0.12.5) |
| OAuth2           | Spring Security OAuth2 Client       |
| API Docs         | springdoc-openapi 3.1 (Swagger UI)  |
| WebSocket        | Spring WebSocket + STOMP            |
| Build            | Maven                               |
| Native Image     | GraalVM CE 25                       |

### Frontend
| Component        | Technology                          |
|------------------|-------------------------------------|
| Library          | React 18                            |
| Language         | TypeScript 5                        |
| Styling          | Tailwind CSS 3                      |
| Bundler          | Webpack 5 + Babel                   |
| Routing          | React Router 6 (HashRouter)         |
| Markdown         | react-markdown + remark-gfm         |
| Diagrams         | Mermaid                             |
| Real-time        | @stomp/stompjs + SockJS             |
| Mocking          | MSW 2 (dev)                         |
| Docs             | TypeDoc                             |

### Mobile
| Component        | Technology                          |
|------------------|-------------------------------------|
| Platform         | Android (Kotlin)                    |
| Min SDK          | API 26                              |

## Prerequisites

- Java 25+ (GraalVM CE 25 for native builds)
- Node.js 18+ (only for frontend dev server)
- Maven 3.9+

## Quick Start

### Full build (backend + frontend)

```bash
mvn clean install
java -jar note-keeper-service/target/note-keeper-service-*.jar
# → http://localhost:8080
```

### Backend only (fast iteration)

```bash
cd note-keeper-service
mvn spring-boot:run
# API: http://localhost:8080/api/v1
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Frontend dev server

```bash
cd note-keeper-web
npm install
npm run dev
# → http://localhost:5173 (proxies /api → localhost:8080)
```

### GraalVM native image

Requires GraalVM 25+ with `native-image` on PATH. Windows: use x64 Native Tools Command Prompt.

```bash
mvn -Pnative native:compile
./note-keeper-service/target/note-keeper -Djavax.xml.accessExternalDTD=all
```

One binary = React UI + Spring API. No JVM needed.

### Docker

```bash
# JVM image
docker build -t note-keeper .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper

# Native image (multi-stage GraalVM)
docker build -f Dockerfile.native -t note-keeper-native .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper-native
```

## Configuration

All config lives in `note-keeper-service/src/main/resources/application.yml`. Override via environment variables or Spring profiles.

### Database

**SQLite (default):** Zero config. Database auto-created at `.data/notekeeper.db`.

**PostgreSQL:**
```bash
SPRING_PROFILES_ACTIVE=postgresql \
DB_HOST=localhost DB_PORT=5432 DB_NAME=notekeeper \
DB_USER=postgres DB_PASSWORD=secret \
java -jar note-keeper-service.jar
```

Schema migration runs automatically on startup.

### Encryption

Generate a key:
```bash
cd note-keeper-service
mvn compile exec:java -Dexec.mainClass="xyz.crearts.note.keeper.service.EncryptionService" -Dexec.classpathScope=compile
```

Set it:
```yaml
app:
  encryption:
    key: "your-base64-key"       # or env APP_ENCRYPTION_KEY
```

> Without a fixed key, a random key is generated on each startup — encrypted notes become unreadable after restart.

### Authentication

- **Local**: Register via `POST /api/v1/auth/register` with email + password
- **Google OAuth2**: Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` env vars
- **JWT secret**: Set `JWT_SECRET` env var (auto-generated if empty)

### Integrations

**Telegram:**
Configure per-user in Settings UI (bot token + chat ID). Supports webhook mode for bidirectional communication.

**DingTalk:**
Configure per-user in Settings UI (webhook URL + optional HMAC secret).

### Backup

Configure in Settings UI or via API:
- Auto-backup with cron schedule (default: daily at 2 AM)
- Configurable retention period
- Manual export/import anytime

### Storage Paths

```yaml
app:
  storage:
    base-dir: ${user.dir}/.data
    attachments-dir: ${app.storage.base-dir}/attachments
    backups-dir: ${app.storage.base-dir}/backups
    db-path: ${app.storage.base-dir}/notekeeper.db
```

## API

Base URL: `/api/v1`. All endpoints except `/auth/**` require `Authorization: Bearer <token>`.

| Domain         | Key Endpoints                                                                                          |
|----------------|--------------------------------------------------------------------------------------------------------|
| Auth           | `POST /auth/register`, `/auth/login`, `/auth/google`                                                   |
| Notes          | CRUD `/notes/{id}`, `/notes/import`, `/notes/{id}/archive`, `/notes/{id}/restore`, `/notes/{id}/history`, `/notes/{id}/share`, `/notes/shared-with-me` |
| Todos          | CRUD `/todos/{id}`, `/todos/{id}/archive`, `/todos/{id}/restore`, `/todos/{id}/share`, `/todos/shared-with-me` |
| Attachments    | `POST /attachments/upload`, `/attachments/upload-batch`, `DELETE /{id}`, `GET /{id}/download`          |
| Search         | `GET /search`, `GET/POST/DELETE /search/queries`                                                       |
| Templates      | `GET/POST/DELETE /templates`                                                                           |
| Backup         | `GET /backup/export`, `POST /backup/import`, `GET /backup/list`, `GET/POST /backup/settings`           |
| Integrations   | `POST /integrations/telegram`, `/integrations/dingtalk`                                                |
| Analytics      | `GET /analytics?timeRange=week\|month\|year`                                                           |
| Settings       | `GET/POST /settings`, `POST /settings/test-notification`                                               |
| Users          | `GET /users/me`, `PUT /users/me`, `GET /users/search`                                                  |
| Tags           | `GET /tags`                                                                                            |
| Telegram       | `POST /telegram/webhook/{secret}`                                                                      |
| Config         | `GET /config/public` (public app config for frontend)                                                  |

Full API docs: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) or Swagger UI at `/swagger-ui.html`.

## Deployment

### CI/CD (Gitea Actions)

| Workflow                | Trigger         | Description                                          |
|-------------------------|-----------------|------------------------------------------------------|
| `tests.yml`             | PR/push         | Run backend tests                                    |
| `deploy.yml`            | push to master  | Build JVM image → push to registry → NAS deploy :9081|
| `deploy-native.yml`     | tag/dispatch    | Build native image → push → NAS deploy :9082         |
| `deploy-k3s.yml`        | manual          | K3s Kubernetes deployment                            |

Registry: HTTP registry at `devops.local:5000` (insecure). Secrets: `REGISTRY_USER`, `REGISTRY_PASSWORD`.

### Reverse Proxy (nginx)

See `nginx.conf` for reference. Key headers: `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`. WebSocket support requires `Upgrade` / `Connection` headers.

Set `APP_PUBLIC_BASE_URL` for correct OAuth redirects and todo links in daily reports.

## Project Structure

```
note-keeper/
├── note-keeper-service/          # Spring Boot backend
│   ├── src/main/java/.../
│   │   ├── controller/           # 15 REST controllers
│   │   ├── service/              # 20 services (business logic, schedulers)
│   │   ├── mapper/               # MyBatis mapper interfaces + type handlers
│   │   ├── model/                # 10 entity classes (Lombok @Data)
│   │   ├── dto/                  # Request/response DTOs
│   │   ├── config/               # Security, JWT, CORS, WebSocket, backup, GraalVM hints
│   │   ├── client/               # TelegramClient, DingTalkClient
│   │   └── exception/            # GlobalExceptionHandler
│   └── src/main/resources/
│       ├── application.yml       # Main config (SQLite/PostgreSQL profiles)
│       ├── schema.sql            # SQLite DDL
│       ├── schema-postgresql.sql # PostgreSQL DDL
│       └── mapper/               # 10 MyBatis XML mapper files
├── note-keeper-web/              # React frontend
│   ├── src/
│   │   ├── App.tsx               # Router + layout + auth guard
│   │   ├── pages/                # 14 page components (lazy-loaded)
│   │   ├── components/           # Reusable UI components
│   │   ├── contexts/             # Theme, Toast, Shortcut providers
│   │   ├── hooks/                # Custom hooks (WebSocket, focus trap, media query)
│   │   ├── utils/                # API client, themes, storage, folder utils
│   │   ├── types/                # TypeScript interfaces
│   │   └── mocks/                # MSW handlers for dev
│   ├── tailwind.config.js
│   └── webpack.config.js
├── android/                      # Android companion app (Kotlin)
├── var/                          # Runtime data (auto-created)
│   ├── notekeeper.db             # SQLite database
│   ├── attachments/              # Uploaded files
│   └── backups/                  # Backup archives
├── Dockerfile                    # JVM Docker image
├── Dockerfile.native             # Native Docker image
├── nginx.conf                    # Reverse proxy reference
└── API_DOCUMENTATION.md          # REST API reference
```

## Development

### Adding a Feature

1. **Backend**: Controller → Service → Mapper interface → MyBatis XML → Model/DTO
2. **Frontend**: Page/component → API calls via `api.ts` → Types in `types/index.ts`
3. **Database**: Update `schema.sql` (SQLite) and `schema-postgresql.sql` if needed
4. **Test**: Backend unit tests in `src/test/`; frontend dev server for manual testing

### Key Conventions

- Controllers call services only — no direct mapper access
- Services call mappers; all DB queries via MyBatis XML
- Models use Lombok `@Data`; DTOs are plain classes
- Exceptions handled centrally in `GlobalExceptionHandler`
- Frontend: no global state library — props + React context only
- Tailwind utilities over custom CSS; use CSS vars for theme-aware styling

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Encrypted notes unreadable after restart | Set `app.encryption.key` in config — without it, random key generated each start |
| Database not created | Check `.data/` directory exists and is writable |
| Attachments fail to upload | Verify `.data/attachments/` exists; check file size limits |
| Backup fails | Ensure `.data/backups/` is writable; check disk space |
| SQLite concurrency issues | Single-connection pool by design; switch to PostgreSQL for multi-instance |
| Native image XML/SQLite errors | Run with `-Djavax.xml.accessExternalDTD=all`; ensure `SqliteJdbcFeature` in build args |
| Telegram/DingTalk native crash | Ensure `NativeRuntimeHints` registers all DTO classes for reflection |

## License

MIT License
