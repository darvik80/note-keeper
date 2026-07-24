# NoteKeeper — Agents.md

Reference guide for AI agents working on this codebase.

---

## Project Overview

Full-stack note-taking application. Multi-module Maven project:
- **note-keeper-service** — Spring Boot 3.5 backend (Java 21, MyBatis, SQLite/PostgreSQL)
- **note-keeper-web** — React 18 + TypeScript frontend (Tailwind CSS, Webpack)

Frontend is bundled into the backend JAR as static resources.

---

## Module Map

```
note-keeper/
├── pom.xml                          # Parent POM (aggregator)
├── note-keeper-service/             # Backend
│   ├── src/main/java/xyz/crearts/note/keeper/
│   │   ├── controller/              # 11 REST controllers
│   │   ├── service/                 # Business logic + ReminderService scheduler
│   │   ├── mapper/                  # 11 MyBatis mappers (interfaces)
│   │   ├── model/                   # 9 entity classes (Lombok @Data)
│   │   ├── dto/                     # Request/response DTOs
│   │   ├── config/                  # Security, JWT filter, CORS, backup scheduler
│   │   ├── client/                  # TelegramClient, DingTalkClient
│   │   └── exception/               # GlobalExceptionHandler
│   ├── src/main/resources/
│   │   ├── application.yml          # Main config (port, DB, encryption key, storage)
│   │   ├── schema.sql               # SQLite DDL
│   │   └── mapper/                  # MyBatis XML mapper files
│   └── POSTGRESQL_AND_BACKUP_GUIDE.md
└── note-keeper-web/                 # Frontend
    ├── src/
    │   ├── App.tsx                  # Router + layout
    │   ├── pages/                   # 14 page components
    │   ├── components/              # Header, Sidebar, FolderTree, MarkdownRenderer, ShareModal, ThemeSelector
    │   ├── contexts/                # ThemeContext, ShortcutContext
    │   ├── utils/                   # api.ts, themes.ts, storage.ts, folderUtils.ts
    │   └── types/index.ts           # All TypeScript interfaces
    ├── tailwind.config.js
    └── webpack.config.js
```

---

## Build & Run

### Full build (backend + frontend together)
```bash
mvn clean install
java -jar note-keeper-service/target/note-keeper-service-*.jar
# → http://localhost:8081
```

### Backend only (faster iteration)
```bash
cd note-keeper-service
mvn spring-boot:run
# API: http://localhost:8081/api/v1
```

### Frontend dev server
```bash
cd note-keeper-web
npm install
npm run dev
# → http://localhost:5173 (proxies /api → localhost:8081)
```

### Frontend production build
```bash
cd note-keeper-web
npm run build    # outputs to dist/, copied into JAR by Maven
```

---

## API Surface (`/api/v1`)

All endpoints except `/api/v1/auth/**` require `Authorization: Bearer <token>`.

| Domain         | Endpoints                                                                                   |
|----------------|---------------------------------------------------------------------------------------------|
| Auth           | POST /auth/register, /auth/login, /auth/google                                              |
| Notes          | CRUD /notes/{id}, /notes/import, /notes/{id}/archive, /notes/{id}/restore, /notes/{id}/history, /notes/{id}/share, /notes/shared-with-me |
| Todos          | CRUD /todos/{id}, /todos/{id}/archive, /todos/{id}/restore, /todos/{id}/share, /todos/shared-with-me |
| Attachments    | POST /attachments/upload, /attachments/upload-batch, DELETE /{id}, GET /{id}/download      |
| Search         | GET /search, GET/POST/DELETE /search/queries                                                |
| Templates      | GET/POST/DELETE /templates                                                                  |
| Backup         | GET /backup/export, POST /backup/import, GET /backup/list, GET /backup/settings, POST /backup/settings |
| Integrations   | POST /integrations/telegram, /integrations/dingtalk                                         |
| Analytics      | GET /analytics?timeRange=week\|month\|year                                                  |

### Filter params
- Notes: `folder`, `tag`, `priority`, `isFavorite`, `isEncrypted`, `isArchived`, `isDeleted`
- Todos: `completed`, `tag`, `priority`, `isFavorite`, `isArchived`, `isDeleted`
- Soft delete: `DELETE /notes/{id}` → moves to trash; `?permanent=true` → hard delete

---

## Database

**Default**: SQLite at `.data/notekeeper.db` (auto-created)
**Optional**: PostgreSQL via Spring profile

### Key Tables
| Table              | Purpose                                                  |
|--------------------|----------------------------------------------------------|
| users              | User accounts (email, Google OAuth, googleId)            |
| user_credentials   | SHA-256 + salt password hashes                           |
| user_settings      | Telegram/DingTalk config, backup settings (JSON fields)  |
| note               | Notes: folder hierarchy, tags (TEXT array), soft delete  |
| note_history       | Version history (created/edited/restored)                |
| todo               | Tasks: due date, location, schedule/recurrence (JSON)    |
| attachment         | Files linked to notes or todos (parent_id + parent_type) |
| note_template      | Reusable templates with category                         |
| saved_query        | User-saved search queries                                |

JSON stored as TEXT columns. MyBatis `StringListTypeHandler` handles `List<String>` ↔ TEXT.

---

## Auth & Security

- **JWT** (JJWT 0.12.5): HS256, 24h expiry, subject = userId (UUID)
- **Filter**: `JwtAuthenticationFilter` reads `Authorization: Bearer` header
- **Passwords**: SHA-256 + random salt (in `UserCredentials`)
- **CORS**: localhost:3000, 5173, 8081 allowed
- **CSRF**: disabled (stateless API)

---

## Encryption

Notes can be encrypted with AES-256-GCM.
Key configured in `application.yml` as Base64-encoded 32-byte key:
```yaml
app:
  encryption:
    key: <base64-32bytes>
```
> If key is absent, a random key is generated at startup — encrypted data is lost on restart.

---

## Frontend Architecture

### State & Routing
- `HashRouter` (client-side routing, `#/path`)
- Auth guard: checks `localStorage.getItem('token')` in `ProtectedRoute`
- User info in `localStorage` as JSON

### API Client (`src/utils/api.ts`)
Centralised client with auto-attached Bearer token. Modules:
`api.auth`, `api.notes`, `api.todos`, `api.templates`, `api.search`, `api.analytics`, `api.attachments`, `api.integrations`

### Theming
12 themes defined in `src/utils/themes.ts`. Applied via CSS custom properties on `:root`. Use CSS vars in components:
```
--color-primary, --color-secondary, --color-background, --color-surface,
--color-text, --color-text-secondary, --color-border, --color-hover
```
Tailwind classes `text-primary`, `bg-surface`, `border-border`, etc. map to these vars via `tailwind.config.js`.

### Responsive Design
- Layout: sidebar (drawer on mobile) + main content
- Breakpoints: Tailwind defaults — `sm:640px`, `lg:1024px`
- Mobile hamburger menu in `App.tsx` (visible below `lg`)
- Sidebar: slide-in overlay on mobile (`isMobileMenuOpen` state in `App.tsx`)

---

## Key Conventions

### Backend
- Controllers call services only — no direct mapper access
- Services call mappers; all DB queries via MyBatis XML mappers in `resources/mapper/`
- Models use Lombok `@Data`; DTOs are plain classes
- Exceptions handled centrally in `GlobalExceptionHandler`
- All endpoints return JSON; errors return `ErrorResponse` DTO

### Frontend
- Pages live in `src/pages/`, reusable components in `src/components/`
- No global state library — props + React context only
- API errors logged to console (no toast system yet)
- Tailwind utilities preferred over custom CSS
- `onKeyDown` for keyboard events (not deprecated `onKeyPress`)

---

## Integrations

### Telegram
Config: `botToken` + `chatId` in `user_settings`.
Sends via `https://api.telegram.org/bot{token}/sendMessage`.

### DingTalk
Config: `webhookUrl` + optional `secret` in `user_settings`.
Auth: HMAC-SHA256 signature (timestamp + `\n` + secret), appended as query params.

### Reminders (`ReminderService`)
- `@Scheduled` every 60s → `findWithDueReminders` (`reminder <= now` AND `notified_at IS NULL OR notified_at < reminder`)
- Sends to `notificationChannels` (default `telegram` if empty)
- Sets `notified_at`, then for `schedule_repeat` in `daily|weekly|monthly` advances `reminder`/`due_date` to next future occurrence and resets `completed=0`
- Stuck repair: `findStuckRecurringReminders` catches todos notified once but never advanced
- Note.reminder = display only — no notify path
- On todo update: reminder change clears `notified_at`

---

## Common Pitfalls

| Area | Issue | Fix |
|------|-------|-----|
| Encryption key | Not set in config → random key on startup → data loss on restart | Set `app.encryption.key` in `application.yml` |
| Tags (List\<String\>) | Stored as TEXT; mapped via `StringListTypeHandler` | Don't bypass MyBatis mapper for these fields |
| Frontend build | `npm run build` must run before `mvn install` if building backend alone | Use root `mvn clean install` for full build |
| SQLite concurrency | Single-connection pool; don't use SQLite in multi-instance deploy | Switch to PostgreSQL for production |
| JWT claims | Subject = userId (UUID string), not email | Use `JwtService.extractUserId()`, not `extractUsername()` |
| Recurring reminders | Mark `notified_at` without advancing `reminder` → fires once forever | Always advance via `ReminderService.advanceRecurringIfNeeded` after notify |
| Reminder edit | Change `reminder` but leave old `notified_at` ≥ new time → never fires | Clear `notified_at` when reminder changes (`TodoService.update`) |

---

## File Locations Quick Reference

| What | Where |
|------|-------|
| Spring Boot entry point | `note-keeper-service/src/main/java/.../NotekeeperApplication.java` |
| Security config + JWT filter | `config/SecurityConfig.java`, `config/JwtAuthenticationFilter.java` |
| Reminder scheduler | `service/ReminderService.java` |
| DB schema (SQLite) | `note-keeper-service/src/main/resources/schema.sql` |
| MyBatis XML mappers | `note-keeper-service/src/main/resources/mapper/*.xml` |
| App config | `note-keeper-service/src/main/resources/application.yml` |
| All TS types | `note-keeper-web/src/types/index.ts` |
| API client | `note-keeper-web/src/utils/api.ts` |
| Theme definitions | `note-keeper-web/src/utils/themes.ts` |
| Router + mobile layout | `note-keeper-web/src/App.tsx` |
