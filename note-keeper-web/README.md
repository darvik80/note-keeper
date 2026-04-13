# NoteKeeper

Open-source Evernote alternative with powerful features for notes and todos management.

## Features

### Notes
- **Markdown & Mermaid Support**: Write notes with full Markdown syntax and create diagrams with Mermaid
- **Folder Structure**: Organize notes in folders and subfolders
- **Tags**: Add multiple tags to notes for easy categorization
- **Priority Levels**: Set priority (low, medium, high) for notes
- **Favorites**: Mark important notes as favorites
- **Encryption**: Encrypt sensitive notes
- **Reminders**: Set reminders for notes
- **Import**: Import from MD, PDF, DOC, DOCX, XLS, CSV files

### Todos
- **Markdown Description**: Write todo descriptions with Markdown support
- **Tags**: Organize todos with tags
- **Priority**: Set priority levels
- **Geolocation**: Add location to todos
- **Schedule**: Set recurring schedules (daily, weekly, monthly)
- **Reminders**: Get notified about upcoming todos
- **Due Dates**: Track deadlines

### Integrations
- **Telegram**: Send notifications to Telegram
- **DingTalk**: Send notifications to DingTalk

## API Documentation

Full REST API documentation is available in `swagger.yaml`. You can view it using:
- [Swagger Editor](https://editor.swagger.io/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)

### API Endpoints

#### Notes
- `GET /api/v1/notes` - Get all notes
- `POST /api/v1/notes` - Create note
- `GET /api/v1/notes/{id}` - Get note by ID
- `PUT /api/v1/notes/{id}` - Update note
- `DELETE /api/v1/notes/{id}` - Delete note
- `POST /api/v1/notes/import` - Import note from file

#### Todos
- `GET /api/v1/todos` - Get all todos
- `POST /api/v1/todos` - Create todo
- `GET /api/v1/todos/{id}` - Get todo by ID
- `PUT /api/v1/todos/{id}` - Update todo
- `DELETE /api/v1/todos/{id}` - Delete todo

#### Integrations
- `POST /api/v1/integrations/telegram` - Send to Telegram
- `POST /api/v1/integrations/dingtalk` - Send to DingTalk

## Development

```bash
# Install dependencies
anpm install

# Start development server
anpm run dev

# Build for production
anpm run build

# Type check
anpm run typecheck
```

## Tech Stack

- React 18
- TypeScript
- React Router
- Tailwind CSS
- React Markdown
- Mermaid
- Webpack 5

## License

MIT
