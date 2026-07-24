/**
 * @module types
 * @category Types
 * Central TypeScript type definitions for the NoteKeeper frontend.
 * Covers domain models, API DTOs, and utility types.
 */

// ---------------------------------------------------------------------------
// Domain models
// ---------------------------------------------------------------------------

/** File attachment linked to a note or todo. */
export interface Attachment {
  /** Unique attachment identifier (UUID). */
  id: string;
  /** Original file name. */
  name: string;
  /** File size in bytes. */
  size: number;
  /** MIME type (e.g. `"image/png"`). */
  type: string;
  /**
   * URL to access the file.
   * - `blob:` URL — local file not yet uploaded to the server.
   * - `/api/v1/attachments/{id}/download` — server-stored file.
   */
  url: string;
  /** When the attachment was uploaded. */
  uploadedAt: Date;
}

/** A note with optional encryption, folder hierarchy, tags, and history. */
export interface Note {
  /** Unique note identifier (UUID). */
  id: string;
  /** Note title. */
  title: string;
  /** Markdown body. Empty string for brand-new notes. */
  content: string;
  /** User-defined tags for filtering and organisation. */
  tags: string[];
  /** Top-level folder path (e.g. `"Work"`). */
  folder: string;
  /** Optional nested folder within `folder` (e.g. `"Q1"`). */
  subfolder?: string;
  /** Priority level for sorting and visual callouts. */
  priority: 'low' | 'medium' | 'high';
  /** Whether the note appears in the Favorites list. */
  isFavorite: boolean;
  /** Whether the note content is AES-256-GCM encrypted server-side. */
  isEncrypted: boolean;
  /** Optional reminder datetime (stored as UTC ISO string on the backend). */
  reminder?: Date;
  /** Files attached to this note. */
  attachments: Attachment[];
  /** `true` after the user archives the note. */
  isArchived: boolean;
  /** `true` after soft-delete; hard-deleted notes are removed from the DB. */
  isDeleted: boolean;
  /** When the note was soft-deleted (populated by the backend). */
  deletedAt?: Date;
  /** Ordered list of previous content versions. */
  history: NoteHistory[];
  /** ID of the {@link NoteTemplate} used to create this note, if any. */
  templateId?: string;
  /** UUID of the user who owns the note. */
  ownerId: string;
  /**
   * JSON-encoded array of user UUIDs this note is shared with.
   * @example '["user-id-1","user-id-2"]'
   */
  sharedWith: string;
  /** When the note was created. */
  createdAt: Date;
  /** When the note was last saved. */
  updatedAt: Date;
}

/** A single snapshot entry in a note's version history. */
export interface NoteHistory {
  /** Unique history entry identifier. */
  id: string;
  /** Full Markdown content at this point in time. */
  content: string;
  /** When this version was recorded. */
  timestamp: Date;
  /** What triggered the history entry. */
  action: 'created' | 'edited' | 'restored';
}

/** A task / todo item with optional scheduling and geolocation. */
export interface Todo {
  /** Unique todo identifier (UUID). */
  id: string;
  /** Short task title. */
  title: string;
  /** Optional Markdown description. */
  description: string;
  /** User-defined tags. */
  tags: string[];
  /** Priority level. */
  priority: 'low' | 'medium' | 'high';
  /** Whether the todo appears in the Favorites list. */
  isFavorite: boolean;
  /** `true` when the task is marked done. */
  completed: boolean;
  /** Optional deadline. */
  dueDate?: Date;
  /** Optional reminder datetime. */
  reminder?: Date;
  /** When the last reminder notification was sent (server-managed). */
  notifiedAt?: Date;
  /** Optional physical location associated with the task. */
  location?: {
    /** Latitude in decimal degrees. */
    lat: number;
    /** Longitude in decimal degrees. */
    lng: number;
    /** Human-readable address string. */
    address: string;
  };
  /** Optional recurrence configuration. */
  schedule?: {
    /** Recurrence interval. `"none"` means no repeat. */
    repeat: 'none' | 'daily' | 'weekly' | 'monthly';
    /** When the recurrence stops (ISO date string from API). */
    endDate?: string;
  };
  /** Files attached to this todo. */
  attachments: Attachment[];
  /** `true` after the user archives the todo. */
  isArchived: boolean;
  /** `true` after soft-delete. */
  isDeleted: boolean;
  /** When the todo was soft-deleted. */
  deletedAt?: Date;
  /** UUID of the owner. */
  ownerId: string;
  /**
   * JSON-encoded array of user UUIDs this todo is shared with.
   * @example '["user-id-1"]'
   */
  sharedWith: string;
  /**
   * Comma-separated list of notification channels for reminders.
   * @example "telegram,dingtalk"
   */
  notificationChannels?: string;
  /** When the todo was created. */
  createdAt: Date;
  /** When the todo was last saved. */
  updatedAt: Date;
}

/** A reusable Markdown template for quickly creating notes. */
export interface NoteTemplate {
  /** Unique template identifier. */
  id: string;
  /** Display name shown in the Templates list. */
  name: string;
  /** Markdown body pre-filled when creating a note from this template. */
  content: string;
  /** Tags automatically applied to notes created from this template. */
  tags: string[];
  /** Grouping category (e.g. `"Meeting"`, `"Personal"`). */
  category: string;
  /** When the template was created. */
  createdAt: Date;
}

/** A user-saved search query with optional filters, shown in Search history. */
export interface SavedQuery {
  /** Unique query identifier. */
  id: string;
  /** User-defined label for this query. */
  name: string;
  /** The raw search text. */
  query: string;
  /** Optional filter constraints applied alongside the text query. */
  filters: {
    /** Limit results to a specific resource type. `"all"` returns both. */
    type?: 'notes' | 'todos' | 'all';
    /** Restrict to notes/todos that have ALL of these tags. */
    tags?: string[];
    /** Restrict to a specific priority level. */
    priority?: 'low' | 'medium' | 'high';
    /** Restrict notes to a specific folder. */
    folder?: string;
  };
  /** When the query was saved. */
  createdAt: Date;
}

/** Persisted user preferences (stored as JSON in `localStorage`). */
export interface Settings {
  /** Telegram bot integration config. */
  telegram: {
    /** Whether Telegram notifications are enabled. */
    enabled: boolean;
    /** Bot API token (from @BotFather). */
    botToken: string;
    /** Target chat or channel ID. */
    chatId: string;
  };
  /** DingTalk webhook integration config. */
  dingtalk: {
    /** Whether DingTalk notifications are enabled. */
    enabled: boolean;
    /** Incoming webhook URL. */
    webhook: string;
    /** Optional HMAC-SHA256 signing secret. */
    secret: string;
  };
  /** SMTP email notification config. */
  email: {
    /** Whether email notifications are enabled. */
    enabled: boolean;
    /** SMTP server host. */
    smtp: string;
    /** SMTP server port (default 587). */
    port: number;
    /** SMTP authentication username. */
    username: string;
    /** SMTP authentication password. */
    password: string;
    /** From address in sent emails. */
    from: string;
    /** Recipient address. */
    to: string;
  };
  /** Currently active theme key (see {@link Theme}). */
  theme: string;
  /** Keyboard shortcut bindings (format: `"ctrl+key"`). */
  shortcuts: {
    /** Create a new note. Default: `"ctrl+n"`. */
    newNote: string;
    /** Create a new todo. Default: `"ctrl+t"`. */
    newTodo: string;
    /** Open the Search page. Default: `"ctrl+k"`. */
    search: string;
    /** Toggle the sidebar. Default: `"ctrl+b"`. */
    toggleSidebar: string;
  };
}

/**
 * A node in the hierarchical folder tree derived from note `folder`/`subfolder` fields.
 * @see buildFolderTree
 */
export interface FolderStructure {
  /** Display name of this folder segment. */
  name: string;
  /**
   * Full slash-separated path from the root.
   * The virtual root node uses the special path `"root"`.
   * @example "Work/Q1"
   */
  path: string;
  /** Nested child folder nodes. */
  subfolders: FolderStructure[];
}

/**
 * Union of all supported UI theme names.
 * Each value maps to a colour palette in {@link themes}.
 */
export type Theme = 'light' | 'dark' | 'green' | 'cyan' | 'blue' | 'purple' | 'darcula' | 'rose' | 'amber' | 'teal' | 'indigo' | 'slate';

// ---------------------------------------------------------------------------
// API DTOs — sent to the backend
// ---------------------------------------------------------------------------

/**
 * Payload for creating or updating a note.
 * All optional fields are omitted when unchanged.
 * @category API
 */
export interface NoteInput {
  /** Note title (required). */
  title: string;
  /** Markdown body. */
  content?: string;
  /** Tag list. */
  tags?: string[];
  /** Top-level folder. */
  folder?: string;
  /** Nested sub-folder. */
  subfolder?: string;
  /** Priority level string (`"low"` | `"medium"` | `"high"`). */
  priority?: string;
  /** Favorite flag. */
  isFavorite?: boolean;
  /** Encryption flag. */
  isEncrypted?: boolean;
  /** ISO 8601 UTC datetime string for the reminder. */
  reminder?: string;
  /** Template ID used to pre-fill content. */
  templateId?: string;
  /** Attachments to persist with the note. */
  attachments?: {
    id: string;
    name: string;
    size: number;
    type: string;
    url: string;
    uploadedAt: string;
  }[];
}

/**
 * Payload for creating or updating a todo.
 * @category API
 */
export interface TodoInput {
  /** Todo title (required). */
  title: string;
  /** Markdown description. */
  description?: string;
  /** Completion flag. */
  completed?: boolean;
  /** Tag list. */
  tags?: string[];
  /** Priority level string. */
  priority?: string;
  /** Favorite flag. */
  isFavorite?: boolean;
  /** ISO 8601 due date string. */
  dueDate?: string;
  /** ISO 8601 reminder datetime string. */
  reminder?: string;
  /** Geolocation data. */
  location?: { lat: number; lng: number; address: string };
  /** Recurrence schedule. */
  schedule?: { repeat: string; endDate?: string };
  /** Comma-separated notification channels (e.g. "telegram,dingtalk"). */
  notificationChannels?: string;
  /** Attachments to persist. */
  attachments?: {
    id: string;
    name: string;
    size: number;
    type: string;
    url: string;
    uploadedAt: string;
  }[];
}

/**
 * Payload for creating a new {@link NoteTemplate}.
 * @category API
 */
export interface NoteTemplateInput {
  /** Template display name. */
  name: string;
  /** Markdown body. */
  content: string;
  /** Default tags. */
  tags?: string[];
  /** Category for grouping. */
  category: string;
}

/**
 * Payload for saving a {@link SavedQuery}.
 * @category API
 */
export interface SavedQueryInput {
  /** Label for the saved query. */
  name: string;
  /** Search text. */
  query: string;
  /** Optional filter constraints. */
  filters?: {
    type?: string;
    tags?: string[];
    priority?: string;
    folder?: string;
  };
}

/**
 * Combined search results returned by `GET /api/v1/search`.
 * @category API
 */
export interface SearchResult {
  /** Matching notes. */
  notes: Note[];
  /** Matching todos. */
  todos: Todo[];
}

/**
 * Analytics summary returned by `GET /api/v1/analytics`.
 * @category API
 */
export interface AnalyticsResponse {
  /** Total notes created in the requested time range. */
  notesCreated: number;
  /** Total todos created in the requested time range. */
  todosCreated: number;
  /** Total todos completed in the requested time range. */
  todosCompleted: number;
  /** Ratio of completed todos to created todos (0–1). */
  completionRate: number;
  /** Most frequently used tags, sorted by count descending. */
  topTags: { tag: string; count: number }[];
  /** Count of notes/todos per priority level (`low`, `medium`, `high`). */
  priorityDistribution: Record<string, number>;
  /**
   * Activity counts per day for the time range.
   * Index 0 = oldest day.
   */
  dailyActivity: number[];
}

/**
 * Common payload for sending a message via an integration.
 * Unused fields may be omitted depending on the target channel.
 * @category API
 */
export interface IntegrationRequest {
  /** Message body text. */
  message: string;
  /** Optional subject / title (used by email). */
  subject?: string;
  /** Telegram bot API token. */
  botToken?: string;
  /** Telegram chat ID. */
  chatId?: string;
  /** DingTalk incoming webhook URL. */
  webhook?: string;
  /** DingTalk HMAC-SHA256 secret. */
  secret?: string;
  /** Email recipient address. */
  to?: string;
}

/**
 * Response from an integration send request.
 * @category API
 */
export interface IntegrationResponse {
  /** `true` when the message was delivered successfully. */
  success: boolean;
  /** Human-readable status message from the backend. */
  message: string;
}

/** Authenticated user profile. */
export interface User {
  /** Unique user identifier (UUID). */
  id: string;
  /** Primary email address. */
  email: string;
  /** Display name. */
  name: string;
  /** Optional avatar image URL. */
  avatarUrl?: string;
  /** Authentication provider. */
  provider: 'local' | 'google';
  /** Google account ID (only set when `provider === "google"`). */
  googleId?: string;
  /** Whether the account is active and can log in. */
  isActive: boolean;
  /** When the account was created. */
  createdAt: Date;
  /** When the account was last updated. */
  updatedAt: Date;
}

/**
 * Payload for `POST /api/v1/auth/register` and `POST /api/v1/auth/login`.
 * @category API
 */
export interface AuthRequest {
  /** User email (local auth). */
  email?: string;
  /** Plaintext password (local auth). */
  password?: string;
  /** Display name (register only). */
  name?: string;
  /** Google account identifier (Google auth). */
  googleId?: string;
  /** Google OAuth ID token (Google auth). */
  googleToken?: string;
}

/**
 * Response from auth endpoints — contains a JWT and the user profile.
 * @category API
 */
export interface AuthResponse {
  /** HS256-signed JWT (24 h expiry). Store in `localStorage` as `"token"`. */
  token: string;
  /** Authenticated user profile. */
  user: User;
}
