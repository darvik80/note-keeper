# NoteKeeper

Self-hosted notes, todos, and a password vault. Web UI ships inside the backend JAR (or GraalVM native binary). Android app in `android/`.

Current version: **2.0.2**. See [CHANGELOG.md](CHANGELOG.md). How to use it: [docs/user-guide.md](docs/user-guide.md).

![Home](docs/screenshots/home.png)

<p align="center">
  <img src="docs/screenshots/notes.png" width="32%" alt="Notes" />
  <img src="docs/screenshots/todos.png" width="32%" alt="Todos" />
  <img src="docs/screenshots/vault.png" width="32%" alt="Vault" />
</p>

## What it does

**Notes** — Markdown (GFM, Mermaid, images), folders, tags, priority, favorites, AES-256-GCM encryption, version history, templates, import from files, share with other users.

**Todos** — Markdown descriptions, tags, priority, due dates, reminders, recurrence (`daily` / `weekly` / `monthly` / `weekdays` / `custom`), completion log. Reminders go to Telegram and/or DingTalk. Recurring `completed` is for the current period only.

**Vault** — Folders and secret items (web + Android). Sync over `/api/v1/vault`.

**Search & views** — Full-text search with saved queries, calendar, analytics, archive, trash, favorites.

**Auth** — Email + password, Google, Yandex. Providers can be linked to one account. JWT (24h) or personal API tokens (`nk_…`, scopes `notes`, `todos`, `vault`, `mcp`).

**Clients** — Bundled web UI, Android (`android/`, minSdk 31), REST `/api/v1`. MCP at `/mcp` (token scope `mcp`). FAR Manager plugins live in a separate repo; pointer in `far-vault/README.md`.

**Ops** — SQLite by default or PostgreSQL. Manual + scheduled backups. GraalVM native image. Docker (JVM and native). Telegram / DingTalk daily reports.

**UI** — 13 themes (default `cogitor`). Responsive sidebar / mobile drawer. Command palette. Toast feedback.

## Tech stack

| Layer | |
|-------|--|
| Backend | Java 25, Spring Boot 4.1, MyBatis, SQLite / PostgreSQL, JWT, GraalVM 25 |
| Web | React 18, TypeScript, Tailwind CSS, Webpack 5 (dev server **:3001**) |
| Android | Kotlin, Jetpack Compose (`android/`) |

## Prerequisites

- Java 25+ (GraalVM CE 25 for native)
- Node.js 18+ (frontend dev only)
- Maven 3.9+

## Quick start

### Full build (backend + frontend)

```bash
mvn clean install
java -jar note-keeper-service/target/note-keeper-service-*.jar
# → http://localhost:8080
```

### Backend only

```bash
cd note-keeper-service
mvn spring-boot:run
# API: http://localhost:8080/api/v1
# Swagger: http://localhost:8080/swagger-ui.html
```

### Frontend dev server

```bash
cd note-keeper-web
npm install
npm run dev
# → http://localhost:3001  (proxies /api → localhost:8080)
```

### GraalVM native image

Needs GraalVM 25+ with `native-image` on PATH. Windows: x64 Native Tools Command Prompt.

```bash
mvn -Pnative native:compile
./note-keeper-service/target/note-keeper -Djavax.xml.accessExternalDTD=all
```

One binary = React UI + API. No JVM at runtime.

### Docker

```bash
docker build -t note-keeper .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper

docker build -f Dockerfile.native -t note-keeper-native .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper-native
```

## Configuration

`note-keeper-service/src/main/resources/application.yml`. Override with env vars or Spring profiles.

### Database

**SQLite (default):** auto-created at `.data/notekeeper.db`.

**PostgreSQL:**

```bash
SPRING_PROFILES_ACTIVE=postgresql \
DB_HOST=localhost DB_PORT=5432 DB_NAME=notekeeper \
DB_USER=postgres DB_PASSWORD=secret \
java -jar note-keeper-service.jar
```

Schema migrates on startup.

### Encryption

```bash
cd note-keeper-service
mvn compile exec:java -Dexec.mainClass="dev.cogitor.note.keeper.service.EncryptionService" -Dexec.classpathScope=compile
```

```yaml
app:
  encryption:
    key: "your-base64-key"   # or APP_ENCRYPTION_KEY
```

Without a fixed key, a random one is generated each start — encrypted notes become unreadable after restart.

### Authentication

| Mode | Config |
|------|--------|
| Local | `POST /api/v1/auth/register` |
| Google | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` |
| Yandex | `YANDEX_CLIENT_ID`, `YANDEX_CLIENT_SECRET` |
| JWT | `JWT_SECRET` (generated if empty) |
| Public URL | `APP_PUBLIC_BASE_URL` — OAuth redirects and links in reports |

OAuth buttons show when `/api/v1/config` reports the provider as enabled. Settings → Account can link/unlink Google and Yandex on an existing user.

### Integrations

Per-user in Settings: Telegram (bot token + chat ID, optional webhook) and DingTalk (webhook URL + optional HMAC secret). Daily report: time, channels, templates.

### Backup

Settings or API: cron schedule (default 02:00), retention, manual export/import, list/download/delete.

### Storage

```yaml
app:
  storage:
    base-dir: ${user.dir}/.data
    attachments-dir: ${app.storage.base-dir}/attachments
    backups-dir: ${app.storage.base-dir}/backups
    db-path: ${app.storage.base-dir}/notekeeper.db
```

## API

Base: `/api/v1`. Except `/auth/**` and public `/config`, send `Authorization: Bearer <jwt or nk_ token>`.

Interactive docs: Swagger at `/swagger-ui.html`. Written reference: [API_DOCUMENTATION.md](API_DOCUMENTATION.md) (partial; Swagger is complete).

| Domain | |
|--------|--|
| Auth | `POST /auth/register`, `/auth/login`; OAuth `GET /auth/oauth/{google\|yandex}`; link `POST/DELETE /auth/link/{provider}`; `GET /auth/me` |
| Notes | CRUD, import, archive, restore, history, share |
| Todos | CRUD, archive, restore, share, `POST /todos/{id}/toggle-complete`, completions |
| Vault | `/vault/folders`, `/vault/items`, `/vault/sync` |
| Attachments | upload, batch, download, delete |
| Search | `/search`, saved queries |
| Templates | CRUD |
| Backup | export, import, list, settings |
| Tokens | `GET/POST /tokens`, `DELETE /tokens/{id}` |
| Integrations | Telegram, DingTalk |
| Analytics | `GET /analytics?timeRange=week\|month\|year` |
| Settings / tags / users | as in Swagger |
| MCP | `POST /mcp` |

Filters — notes: `folder`, `tag`, `priority`, `isFavorite`, `isEncrypted`, `isArchived`, `isDeleted`. Todos: `completed`, `tag`, `priority`, `isFavorite`, `isArchived`, `isDeleted`. Soft delete: `DELETE /notes/{id}` → trash; `?permanent=true` → hard delete.

## Android

Open `android/` in Android Studio. `applicationId`: `dev.cogitor.notekeeper.v2`.

Notes, todos, vault, search. Email/password plus Google and Yandex (`notekeeper://oauth`). Swipe: note → archive; todo → done / archive.

Details: [android/README.md](android/README.md).

## Deployment

| Workflow | Trigger | |
|----------|---------|--|
| `tests.yml` | PR/push | Backend tests |
| `deploy.yml` | push to master | JVM image → registry → NAS **:9081** |
| `deploy-native.yml` | tag/dispatch | Native image → NAS **:9082** |
| `deploy-k3s.yml` | manual | K3s |

Registry: `devops.local:5000` (HTTP, insecure). Secrets: `REGISTRY_USER`, `REGISTRY_PASSWORD`.

Reverse proxy: `nginx.conf`. Needs `Host`, `X-Forwarded-*`, WebSocket `Upgrade` / `Connection`. Set `APP_PUBLIC_BASE_URL` in production.

## Layout

```
note-keeper/
├── note-keeper-service/     Spring Boot API + static web
├── note-keeper-web/         React UI (copied into the JAR)
├── android/                 Android app (Compose)
├── far-vault/               Pointer to FAR plugins (other repo)
├── docs/                    User guide, PRD, screenshots
├── Dockerfile / Dockerfile.native
├── nginx.conf
├── CHANGELOG.md
└── API_DOCUMENTATION.md
```

## Conventions

- Controllers → services → MyBatis XML. No mapper calls from controllers.
- Lombok `@Data` on models; DTOs are plain classes.
- `GlobalExceptionHandler` → `ErrorResponse` JSON.
- Web: pages in `src/pages/`, no global store (props + context). Tailwind + CSS variables for themes.

## Troubleshooting

| Problem | |
|---------|--|
| Encrypted notes unreadable after restart | Set `app.encryption.key` / `APP_ENCRYPTION_KEY` |
| DB / attachments / backups fail | `.data/` (and subdirs) must exist and be writable |
| SQLite under concurrent writers | Single-connection pool; use PostgreSQL for multi-instance |
| Native XML/SQLite errors | `-Djavax.xml.accessExternalDTD=all`; `SqliteJdbcFeature` in native args |
| Native `CPU ISA level is lower than required` | Image `25-ol9`, `-march=x86-64-v2` (Synology is v2) |
| Native Telegram/DingTalk Jackson errors | Public nested response DTOs + `NativeRuntimeHints` |
| OAuth redirect wrong | Set `APP_PUBLIC_BASE_URL` |

## License

MIT
