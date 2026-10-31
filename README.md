# NoteKeeper

**English** · [Русский](README.ru.md)

Self-hosted notes, todos, and a password vault. The web UI is bundled with the server (JVM JAR or a GraalVM native binary). An Android client lives in `android/` and talks to the same API.

Current version: **2.0.3**. Release notes: [CHANGELOG.md](CHANGELOG.md). How to use the app: [docs/user-guide.md](docs/user-guide.md) · [на русском](docs/user-guide.ru.md).

**Try it:** a public test instance runs at [https://note.cogitor.dev](https://note.cogitor.dev). Use it to look around. Do not put real secrets there — the data is not yours to keep.

![Home](docs/screenshots/home.png)

<p align="center">
  <img src="docs/screenshots/notes.png" width="32%" alt="Notes" />
  <img src="docs/screenshots/todos.png" width="32%" alt="Todos" />
  <img src="docs/screenshots/vault.png" width="32%" alt="Vault" />
</p>

## What you get

Notes are Markdown with GitHub-flavored extras: tables, task lists, fenced code, Mermaid diagrams, and images. You can file them in folders, tag them, mark favorites, encrypt a note with AES-256-GCM, keep version history, start from a template, import `.txt` / `.md`, and share with other users on the same server.

Todos also take Markdown. They have tags, priority, due dates, and reminders. Recurrence can be daily, weekly, monthly, weekdays, or a custom set of weekdays. Completing a recurring todo means “done for this period”; the next period it is open again. Reminders and an optional daily digest go out through Telegram and/or DingTalk.

The vault stores folders of secrets (passwords, SSH keys, API tokens, secure notes) on the server. Web and Android both sync over `/api/v1/vault`.

Around that: full-text search with saved queries, a calendar, analytics, archive, trash, and favorites. Thirteen themes ship with the UI (default **cogitor**), with a responsive sidebar that becomes a drawer on a phone, a command palette, and toast feedback.

Sign-in is email and password. Google and Yandex are optional: leave the client id empty and those buttons stay hidden. You can link either provider to an existing account. Sessions use JWT (24 hours). Personal API tokens (`nk_…`) can be scoped to `notes`, `todos`, `vault`, or `mcp`.

Clients: the bundled web app, Android (`android/`, minSdk 31), REST at `/api/v1`, and MCP at `/mcp` (token scope `mcp`). FAR Manager plugins live in a separate repository; see `far-vault/README.md`.

Operations: SQLite by default, PostgreSQL if you need it, manual and scheduled backups, Docker (JVM and native), and a Debian package that ships the native binary.

## Tech stack

| Layer | |
|-------|--|
| Backend | Java 25, Spring Boot 4.1, MyBatis, SQLite / PostgreSQL, JWT, GraalVM 25 |
| Web | React 18, TypeScript, Tailwind CSS, Webpack 5 (dev server **:3001**) |
| Android | Kotlin, Jetpack Compose (`android/`) |

## Prerequisites

- Java 25+ (GraalVM CE 25 if you want a native image)
- Node.js 18+ (only for frontend development)
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

Needs GraalVM 25+ with `native-image` on `PATH`. On Windows, use an x64 Native Tools Command Prompt.

```bash
mvn -Pnative native:compile
./note-keeper-service/target/note-keeper -Djavax.xml.accessExternalDTD=all
```

One binary contains the React UI and the API. There is no JVM at runtime.

### Docker

```bash
docker build -t note-keeper .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper

docker build -f Dockerfile.native -t note-keeper-native .
docker run -d -p 8080:8080 -v ./var:/app/.data note-keeper-native
```

### Debian package

The `.deb` wraps the GraalVM native binary, so you do not need a JVM on the host. CI builds it with `Dockerfile.native` (`-march=x86-64-v2`). You need Docker, and either `dpkg-deb` or Docker again to pack the archive.

```bash
bash deb/build.sh          # dist/note-keeper_<version>_amd64.deb
sudo dpkg -i dist/note-keeper_*.deb
```

The installer asks for the public URL (include `https://`), listen port (default 8080), database (`sqlite` or `postgresql`), and optional Google / Yandex client id, secret, and callback. Leave OAuth fields empty to disable that provider. To change answers later:

```bash
sudo dpkg-reconfigure note-keeper
```

Noninteractive installs (`DEBIAN_FRONTEND=noninteractive`) keep the defaults: SQLite, port 8080, no OAuth. Values are written to `/opt/note-keeper/etc/application.yaml` (mode 0640).

Gitea Actions (`.gitea/workflows/build-deb.yml`, runner `devops.local`) publishes the package to `Cogitor/packages` as generic `cogitor-products`.

## Configuration

The baseline is `note-keeper-service/src/main/resources/application.yml`. Override it with environment variables, Spring profiles, or the overlay YAML the Debian package writes.

### Database

SQLite is the default. The file is created at `.data/notekeeper.db` (or under `/opt/note-keeper/var` in the Debian layout).

PostgreSQL:

```bash
SPRING_PROFILES_ACTIVE=postgresql \
DB_HOST=localhost DB_PORT=5432 DB_NAME=notekeeper \
DB_USER=postgres DB_PASSWORD=secret \
java -jar note-keeper-service.jar
```

Schema is applied on startup.

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

If you do not set a key, a random one is generated at every start, and encrypted notes become unreadable after a restart.

### Authentication

| Mode | Config |
|------|--------|
| Local | `POST /api/v1/auth/register` |
| Google | optional `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` |
| Yandex | optional `YANDEX_CLIENT_ID`, `YANDEX_CLIENT_SECRET`, `YANDEX_REDIRECT_URI` |
| JWT | `JWT_SECRET` (generated if empty) |
| Public URL | `APP_PUBLIC_BASE_URL` — must include `https://` or `http://`. Used for OAuth redirects and links in reports |

OAuth buttons appear when `/api/v1/config` reports that provider as enabled. After you are signed in, **Settings → Account** can link or unlink Google and Yandex.

### Integrations

Each user configures Telegram (bot token + chat ID, optional webhook) and DingTalk (webhook URL + optional HMAC secret) in Settings. The daily report has a time, channels, and templates.

### Backup

From Settings or the API: a cron schedule (default 02:00), retention, manual export/import, and list/download/delete of backup files.

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

Base path: `/api/v1`. Everything except `/auth/**` and the public `/config` endpoint expects `Authorization: Bearer <jwt or nk_ token>`.

Interactive docs: Swagger at `/swagger-ui.html`. A written (partial) reference is [API_DOCUMENTATION.md](API_DOCUMENTATION.md); Swagger is the complete one.

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
| Metrics | public `GET /actuator/health`, `GET /actuator/prometheus` (Prometheus scrape). JVM/HTTP/Tomcat/Hikari plus business counters (`notekeeper_notes_added_total`, `notekeeper_notes_archived_total`, `notekeeper_auth_logins_total`, `notekeeper_auth_rejections_total`, `notekeeper_telegram_webhook_errors_total`, `notekeeper_reminders_sent_total`, `notekeeper_backups_total`, `notekeeper_backup_duration_seconds`) and row gauges `notekeeper_records_*` |

Note filters: `folder`, `tag`, `priority`, `isFavorite`, `isEncrypted`, `isArchived`, `isDeleted`. Todo filters: `completed`, `tag`, `priority`, `isFavorite`, `isArchived`, `isDeleted`. Soft delete (`DELETE /notes/{id}`) moves the item to trash; `?permanent=true` deletes it for good.

## Android

Open `android/` in Android Studio. `applicationId` is `dev.cogitor.notekeeper.v2`.

The app covers notes, todos, vault, and search. Sign-in is email/password plus optional Google and Yandex (`notekeeper://oauth`). Swipe a note to archive; swipe a todo to mark it done or archive it.

Point the client at your own server, or at the test instance [https://note.cogitor.dev](https://note.cogitor.dev). Details: [android/README.md](android/README.md).

## Deployment

| Workflow | Trigger | |
|----------|---------|--|
| `tests.yml` | PR/push | Backend tests |
| `deploy.yml` | push to master | JVM image → registry → NAS **:9081** |
| `deploy-native.yml` | tag/dispatch | Native image → NAS **:9082** |
| `deploy-k3s.yml` | manual | K3s |

Registry: `devops.local:5000` (HTTP, insecure). Secrets: `REGISTRY_USER`, `REGISTRY_PASSWORD`.

Put a reverse proxy in front (`nginx.conf`). It must pass `Host`, `X-Forwarded-*`, and WebSocket `Upgrade` / `Connection`. Set `APP_PUBLIC_BASE_URL` to the public origin **with scheme**, for example `https://note.cogitor.dev`.

Prometheus scrape (no auth):

```yaml
scrape_configs:
  - job_name: note-keeper
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["localhost:8080"]
```

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

Controllers call services; services call MyBatis XML mappers. Controllers never talk to mappers directly. Models use Lombok `@Data`; DTOs are plain classes. `GlobalExceptionHandler` returns `ErrorResponse` JSON.

On the web, pages live in `src/pages/`. There is no global store — props and React context only. Themes use Tailwind plus CSS variables.

## Troubleshooting

| Problem | What to do |
|---------|------------|
| Encrypted notes unreadable after restart | Set a stable `app.encryption.key` / `APP_ENCRYPTION_KEY` |
| DB / attachments / backups fail | `.data/` (and its subdirs) must exist and be writable |
| SQLite under concurrent writers | The pool is a single connection; use PostgreSQL for more than one instance |
| Native XML/SQLite errors | Run with `-Djavax.xml.accessExternalDTD=all`; native args include `SqliteJdbcFeature` |
| Native `CPU ISA level is lower than required` | Build with image `25-ol9` and `-march=x86-64-v2` (Synology is v2) |
| Native Telegram/DingTalk Jackson errors | Keep nested response DTOs public and listed in `NativeRuntimeHints` |
| OAuth redirect goes to a relative path | Set `APP_PUBLIC_BASE_URL` including `https://` |

## License

MIT
