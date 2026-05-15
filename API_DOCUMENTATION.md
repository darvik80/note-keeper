# NoteKeeper REST API Documentation

Complete API reference for building custom clients.

## Base URL

```
http://localhost:8080/api/v1
```

## Authentication

All endpoints (except `/auth`) require JWT token in `Authorization` header:

```
Authorization: Bearer <token>
```

### Get Token

**POST** `/auth/login`

```json
{
  "email": "user@example.com",
  "password": "your-password"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "name": "User Name"
  }
}
```

---

## Notes API

### List Notes

**GET** `/notes`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| folder | string | Filter by folder name |
| tag | string | Filter by tag |
| priority | string | Filter by priority (low/medium/high) |
| isFavorite | boolean | Filter favorites |
| isEncrypted | boolean | Filter encrypted notes |
| isArchived | boolean | Filter archived notes |
| isDeleted | boolean | Filter deleted notes |

**Response:**
```json
[
  {
    "id": "note-uuid",
    "title": "Note Title",
    "content": "Note content...",
    "tags": ["tag1", "tag2"],
    "folder": "default",
    "subfolder": "subfolder",
    "priority": "medium",
    "isFavorite": false,
    "isEncrypted": false,
    "isArchived": false,
    "isDeleted": false,
    "reminder": "2024-01-01T12:00:00Z",
    "ownerId": "user-uuid",
    "sharedWith": "[]",
    "createdAt": "2024-01-01T10:00:00Z",
    "updatedAt": "2024-01-01T10:00:00Z",
    "attachments": [],
    "history": []
  }
]
```

### Create Note

**POST** `/notes`

**Request Body:**
```json
{
  "title": "New Note",
  "content": "Note content...",
  "tags": ["tag1", "tag2"],
  "folder": "default",
  "subfolder": "subfolder",
  "priority": "medium",
  "isFavorite": false,
  "isEncrypted": false,
  "reminder": "2024-01-01T12:00:00Z"
}
```

**Response:** `201 Created` + Note object

### Get Note by ID

**GET** `/notes/{id}`

**Response:** Note object

### Update Note

**PUT** `/notes/{id}`

**Request Body:** Same as Create Note

**Response:** Updated Note object

### Delete Note

**DELETE** `/notes/{id}`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| permanent | boolean | false | If true, permanently delete (no recovery) |

**Response:** `204 No Content`

### Archive Note

**POST** `/notes/{id}/archive`

**Response:** Archived Note object

### Restore Note

**POST** `/notes/{id}/restore`

**Response:** Restored Note object

### Get Note History

**GET** `/notes/{id}/history`

**Response:**
```json
[
  {
    "id": "history-uuid",
    "noteId": "note-uuid",
    "content": "Previous content...",
    "timestamp": "2024-01-01T10:00:00Z",
    "action": "edited"
  }
]
```

### Import Note from File

**POST** `/notes/import`

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| file | file | Text file to import |
| folder | string | Target folder (optional) |
| subfolder | string | Target subfolder (optional) |

**Response:** `201 Created` + Note object

### Get Shared Notes

**GET** `/notes/shared-with-me`

**Response:** List of notes shared with current user

### Share Note

**POST** `/notes/{id}/share`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | string | User ID to share with |

**Response:** Updated Note object

### Unshare Note

**DELETE** `/notes/{id}/share`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | string | User ID to unshare from |

**Response:** Updated Note object

---

## Todos API

### List Todos

**GET** `/todos`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| completed | boolean | Filter by completion status |
| tag | string | Filter by tag |
| priority | string | Filter by priority |
| isFavorite | boolean | Filter favorites |
| isArchived | boolean | Filter archived |
| isDeleted | boolean | Filter deleted |

**Response:** List of Todo objects

### Create Todo

**POST** `/todos`

**Request Body:**
```json
{
  "title": "New Todo",
  "description": "Todo description...",
  "tags": ["tag1"],
  "priority": "medium",
  "isFavorite": false,
  "completed": false,
  "dueDate": "2024-01-01T12:00:00Z",
  "reminder": "2024-01-01T10:00:00Z",
  "notificationChannels": "telegram,dingtalk",
  "location": "Location name",
  "schedule": {
    "repeat": "daily",
    "endDate": "2024-12-31T23:59:59Z"
  }
}
```

**Response:** `201 Created` + Todo object

### Notification Channels

The `notificationChannels` field in Todo specifies which channels to send reminders to.

**Format:** Comma-separated string

**Available Channels:**
| Channel | Description |
|---------|-------------|
| `telegram` | Send notification via Telegram bot |
| `dingtalk` | Send notification via DingTalk webhook |

**Examples:**
```json
// Send to Telegram only
{ "notificationChannels": "telegram" }

// Send to DingTalk only
{ "notificationChannels": "dingtalk" }

// Send to both channels
{ "notificationChannels": "telegram,dingtalk" }

// Send to neither (no notifications)
{ "notificationChannels": "" }

// Default (if not specified, sends to both)
{ "notificationChannels": null }
```

**How it works:**
1. Set `reminder` field to schedule a notification time
2. Set `notificationChannels` to specify where to send the notification
3. The server checks for due reminders every minute
4. Notifications are sent via configured Telegram/DingTalk credentials (set in Settings UI)
5. After notification is sent, `notifiedAt` field is updated

**Note:** Telegram and DingTalk credentials must be configured in Settings > Integrations for notifications to work.

### Get Todo by ID

**GET** `/todos/{id}`

**Response:** Todo object

### Update Todo

**PUT** `/todos/{id}`

**Request Body:** Same as Create Todo

**Response:** Updated Todo object

### Delete Todo

**DELETE** `/todos/{id}`

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| permanent | boolean | false | If true, permanently delete |

**Response:** `204 No Content`

### Archive Todo

**POST** `/todos/{id}/archive`

**Response:** Archived Todo object

### Restore Todo

**POST** `/todos/{id}/restore`

**Response:** Restored Todo object

### Get Shared Todos

**GET** `/todos/shared-with-me`

**Response:** List of todos shared with current user

### Share Todo

**POST** `/todos/{id}/share`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | string | User ID to share with |

**Response:** Updated Todo object

### Unshare Todo

**DELETE** `/todos/{id}/share`

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | string | User ID to unshare from |

**Response:** Updated Todo object

---

## Attachments API

### Upload File

**POST** `/attachments/upload`

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| file | file | File to upload |
| parentId | string | Parent note/todo ID |
| parentType | string | "note" or "todo" |

**Response:**
```json
{
  "id": "attachment-uuid",
  "parentId": "note-uuid",
  "parentType": "note",
  "name": "file.txt",
  "size": 1024,
  "type": "text/plain",
  "url": "/api/v1/attachments/attachment-uuid/download",
  "uploadedAt": "2024-01-01T10:00:00Z"
}
```

### Upload Multiple Files

**POST** `/attachments/upload-batch`

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| files | file[] | Multiple files |
| parentId | string | Parent note/todo ID |
| parentType | string | "note" or "todo" |

**Response:** List of Attachment objects

### Delete Attachment

**DELETE** `/attachments/{attachmentId}`

**Response:** `204 No Content`

### Download File

**GET** `/attachments/{attachmentId}/download`

**Response:** File content (binary)

---

## Backup API

### Export Data

**GET** `/backup/export`

**Response:** JSON file download with all data

### Import Data

**POST** `/backup/import`

**Form Data:**
| Field | Type | Description |
|-------|------|-------------|
| file | file | Backup JSON file |

**Response:**
```json
{
  "status": "success",
  "message": "Data imported successfully"
}
```

### List Backups

**GET** `/backup/list`

**Response:**
```json
[
  {
    "filename": "backup_2024-01-01.json",
    "size": 10240,
    "createdAt": "2024-01-01T10:00:00Z"
  }
]
```

### Delete Backup

**DELETE** `/backup/delete/{filename}`

**Response:**
```json
{
  "status": "success",
  "message": "Backup deleted: backup_2024-01-01.json"
}
```

### Download Backup

**GET** `/backup/download/{filename}`

**Response:** Backup file download

### Get Backup Settings

**GET** `/backup/settings`

**Response:**
```json
{
  "enabled": true,
  "cron": "0 0 2 * * *",
  "retentionDays": 30
}
```

### Update Backup Settings

**POST** `/backup/settings`

**Request Body:**
```json
{
  "enabled": true,
  "cron": "0 0 2 * * *",
  "retentionDays": 30
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Backup settings updated successfully"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/notes"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/v1/notes"
}
```

### 404 Not Found
```json
{
  "timestamp": "2024-01-01T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Note not found: note-uuid",
  "path": "/api/v1/notes/note-uuid"
}
```

---

## Example Client (JavaScript/TypeScript)

```typescript
class NoteKeeperClient {
  private baseUrl: string;
  private token: string;

  constructor(baseUrl: string, token: string) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return response.json();
  }

  // Notes
  async getNotes(filters?: { folder?: string; tag?: string; priority?: string }): Promise<Note[]> {
    const params = new URLSearchParams();
    if (filters?.folder) params.append('folder', filters.folder);
    if (filters?.tag) params.append('tag', filters.tag);
    if (filters?.priority) params.append('priority', filters.priority);
    
    return this.request(`/notes${params.toString() ? '?' + params.toString() : ''}`);
  }

  async createNote(note: NoteInput): Promise<Note> {
    return this.request('/notes', {
      method: 'POST',
      body: JSON.stringify(note),
    });
  }

  async getNote(id: string): Promise<Note> {
    return this.request(`/notes/${id}`);
  }

  async updateNote(id: string, note: NoteInput): Promise<Note> {
    return this.request(`/notes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(note),
    });
  }

  async deleteNote(id: string, permanent: boolean = false): Promise<void> {
    await fetch(`${this.baseUrl}/notes/${id}?permanent=${permanent}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${this.token}` },
    });
  }

  // Todos
  async getTodos(filters?: { completed?: boolean; tag?: string }): Promise<Todo[]> {
    const params = new URLSearchParams();
    if (filters?.completed !== undefined) params.append('completed', String(filters.completed));
    if (filters?.tag) params.append('tag', filters.tag);
    
    return this.request(`/todos${params.toString() ? '?' + params.toString() : ''}`);
  }

  async createTodo(todo: TodoInput): Promise<Todo> {
    return this.request('/todos', {
      method: 'POST',
      body: JSON.stringify(todo),
    });
  }

  // Attachments
  async uploadAttachment(file: File, parentId: string, parentType: 'note' | 'todo'): Promise<Attachment> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('parentId', parentId);
    formData.append('parentType', parentType);

    const response = await fetch(`${this.baseUrl}/attachments/upload`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${this.token}` },
      body: formData,
    });

    return response.json();
  }

  // Backup
  async exportBackup(): Promise<Blob> {
    const response = await fetch(`${this.baseUrl}/backup/export`, {
      headers: { 'Authorization': `Bearer ${this.token}` },
    });
    return response.blob();
  }

  async importBackup(file: File): Promise<{ status: string; message: string }> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${this.baseUrl}/backup/import`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${this.token}` },
      body: formData,
    });

    return response.json();
  }
}

// Usage
const client = new NoteKeeperClient('http://localhost:8080/api/v1', 'your-jwt-token');

// Create a note
const note = await client.createNote({
  title: 'My Note',
  content: 'Note content...',
  tags: ['important'],
  folder: 'default',
  priority: 'high',
});

// Get all notes
const notes = await client.getNotes();

// Upload attachment
const attachment = await client.uploadAttachment(file, note.id, 'note');

// Export backup
const backup = await client.exportBackup();
```

---

## Data Models

### Note
```typescript
interface Note {
  id: string;
  title: string;
  content: string;
  tags: string[];
  folder: string;
  subfolder?: string;
  priority: 'low' | 'medium' | 'high';
  isFavorite: boolean;
  isEncrypted: boolean;
  isArchived: boolean;
  isDeleted: boolean;
  deletedAt?: string;
  reminder?: string;
  templateId?: string;
  ownerId: string;
  sharedWith: string;
  createdAt: string;
  updatedAt: string;
  attachments?: Attachment[];
  history?: NoteHistory[];
}
```

### Todo
```typescript
interface Todo {
  id: string;
  title: string;
  description: string;
  completed: boolean;
  tags: string[];
  priority: 'low' | 'medium' | 'high';
  isFavorite: boolean;
  isArchived: boolean;
  isDeleted: boolean;
  deletedAt?: string;
  dueDate?: string;
  reminder?: string;
  notifiedAt?: string;
  notificationChannels?: string;
  locationLat?: number;
  locationLng?: number;
  locationAddress?: string;
  scheduleRepeat: 'none' | 'daily' | 'weekly' | 'monthly';
  scheduleEndDate?: string;
  ownerId: string;
  sharedWith: string;
  createdAt: string;
  updatedAt: string;
  attachments?: Attachment[];
}
```

### Attachment
```typescript
interface Attachment {
  id: string;
  parentId: string;
  parentType: 'note' | 'todo';
  name: string;
  size: number;
  type: string;
  url: string;
  uploadedAt: string;
}
```
