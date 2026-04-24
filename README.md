# NoteKeeper

Open-source Evernote alternative with powerful features for notes and todos management.

## Features

- 📝 **Notes Management** - Create, edit, organize notes with Markdown support
- ✅ **Todo Management** - Task tracking with priorities, due dates, reminders
- 📁 **Folder Organization** - Hierarchical folder/subfolder structure
- 🔍 **Full-text Search** - Quick search across all notes and todos
- 📅 **Calendar View** - Visual calendar for tasks with due dates
- 📊 **Analytics** - Statistics and insights
- ⭐ **Favorites** - Mark important items
- 📦 **Archive** - Archive completed items
- 🗑️ **Trash** - Soft delete with recovery
- 🔐 **Encryption** - Encrypt sensitive notes
- 📎 **Attachments** - File attachments for notes and todos
- 🔄 **Backup & Restore** - Automatic backups with configurable retention
- 🔔 **Reminders** - Email/Telegram/DingTalk notifications
- 👥 **Sharing** - Share notes and todos with other users
- 🌙 **Dark Mode** - Theme switching
- ⌨️ **Keyboard Shortcuts** - Customizable hotkeys
- 📱 **Responsive Design** - Works on desktop and mobile

## Tech Stack

### Backend
- Java 25
- Spring Boot 3.5
- MyBatis
- SQLite (default) / PostgreSQL
- Maven

### Frontend
- React 18
- TypeScript
- Tailwind CSS
- Vite

## Prerequisites

- Java 25+
- Node.js 18+
- Maven 3.6+

## Configuration

### 1. Storage Paths

Edit `note-keeper-service/src/main/resources/application.yml`:

```yaml
app:
  storage:
    base-dir: ${user.dir}/../var
    attachments-dir: ${app.storage.base-dir}/attachments
    backups-dir: ${app.storage.base-dir}/backups
    data-dir: ${app.storage.base-dir}/data
    db-path: ${app.storage.data-dir}/notekeeper.db
```

**Directory structure:**
```
var/
├── data/          # SQLite database
├── attachments/   # Uploaded files
└── backups/       # Backup archives
```

### 2. Database

**SQLite (default):**
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${app.storage.db-path}
```

**PostgreSQL:**
```yaml
spring:
  profiles:
    active: postgresql
  datasource:
    url: jdbc:postgresql://localhost:5432/notekeeper
    username: your_username
    password: your_password
```

Run schema migration:
```bash
psql -U your_username -d notekeeper -f note-keeper-service/src/main/resources/schema-postgresql.sql
```

### 3. Encryption (Optional but Recommended)

Generate encryption key using Java:
```bash
cd note-keeper-service
mvn compile exec:java -Dexec.mainClass="xyz.crearts.note.keeper.service.EncryptionService" -Dexec.classpathScope=compile
```

Or manually in code:
```java
String key = EncryptionService.generateKey();
System.out.println(key); // Copy this key
```

Set the key in `application.yml`:
```yaml
app:
  encryption:
    key: "your-generated-base64-key-here"
```

Or use environment variable:
```bash
export ENCRYPTION_KEY="your-generated-base64-key-here"
```

**Important:** 
- Without a fixed key, encrypted notes will be unreadable after restart
- Store key securely (password manager, secrets vault)
- Key is 256-bit AES (Base64 encoded = 44 characters)
- Once set, never change it or you'll lose access to encrypted notes

### 4. Authentication

Default credentials (development only):
- Username: `admin`
- Password: Auto-generated on first start (check logs)

Production: Configure OAuth (Google) or LDAP.

### 4. Integrations (Optional)

**Telegram Notifications:**
```yaml
app:
  integrations:
    telegram:
      enabled: true
      bot-token: YOUR_BOT_TOKEN
      chat-id: YOUR_CHAT_ID
```

**DingTalk Notifications:**
```yaml
app:
  integrations:
    dingtalk:
      enabled: true
      webhook: YOUR_WEBHOOK_URL
      secret: YOUR_SECRET
```

### 5. Backup Settings

Configure in Settings UI or `application.yml`:
```yaml
app:
  backup:
    auto-enabled: true
    cron: "0 0 2 * * *"  # Daily at 2 AM
    retention-days: 30
```

## Installation

### Backend

```bash
cd note-keeper-service
mvn clean install
mvn spring-boot:run
```

Server starts on `http://localhost:8080`

### Frontend

```bash
cd note-keeper-web
npm install
npm run dev
```

Or build production:
```bash
mvn clean install -pl note-keeper-web
```

Frontend served by backend at `http://localhost:8080`

## API Documentation

Access OpenAPI docs at: `http://localhost:8080/swagger-ui.html`

Key endpoints:
- `GET /api/v1/notes` - List notes
- `POST /api/v1/notes` - Create note
- `GET /api/v1/todos` - List todos
- `POST /api/v1/backup/export` - Export backup
- `POST /api/v1/backup/import` - Import backup

## Keyboard Shortcuts

Customize in Settings > Shortcuts:
- `Ctrl+N` - New note
- `Ctrl+T` - New todo
- `Ctrl+K` - Search
- `Ctrl+B` - Toggle sidebar
- `Esc` - Exit fullscreen

## Development

### Project Structure

```
note-keeper/
├── note-keeper-service/    # Spring Boot backend
│   ├── src/main/java/
│   │   └── xyz/crearts/note/keeper/
│   │       ├── controller/  # REST controllers
│   │       ├── service/     # Business logic
│   │       ├── mapper/      # MyBatis mappers
│   │       ├── dto/         # Data transfer objects
│   │       └── config/      # Configuration
│   └── src/main/resources/
│       ├── application.yml  # Configuration
│       ├── schema.sql       # Database schema
│       └── mapper/          # MyBatis XML
├── note-keeper-web/        # React frontend
│   ├── src/
│   │   ├── pages/          # Page components
│   │   ├── components/     # Reusable components
│   │   ├── utils/          # Utilities
│   │   └── types/          # TypeScript types
│   └── package.json
└── var/                    # Runtime data (auto-created)
    ├── data/
    ├── attachments/
    └── backups/
```

### Adding New Features

1. Backend: Add controller → service → mapper → DTO
2. Frontend: Add page/component → update API calls
3. Update schema if needed
4. Test both modules

## Troubleshooting

**Database not created:**
- Check `var/data/` directory exists
- Verify write permissions
- Check logs for SQLite errors

**Attachments not uploading:**
- Verify `var/attachments/` exists
- Check file size limits
- Review backend logs

**Backup fails:**
- Ensure `var/backups/` is writable
- Check disk space
- Review cron expression format

## License

MIT License
