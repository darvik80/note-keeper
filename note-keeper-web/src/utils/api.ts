/**
 * @module api
 * @category API
 * Centralised HTTP client for all NoteKeeper backend calls (`/api/v1`).
 *
 * Every method automatically attaches the JWT stored in `localStorage` as a
 * `Authorization: Bearer <token>` header.  Authentication methods (`register`,
 * `login`, `loginWithGoogle`) are the only calls that do **not** require an
 * existing token.
 *
 * @example
 * ```ts
 * import { api } from '../utils/api';
 *
 * const notes = await api.notes.getAll({ folder: 'Work' });
 * ```
 */
import {
  Note, Todo, NoteTemplate, SavedQuery, NoteHistory,
  NoteInput, TodoInput, NoteTemplateInput, SavedQueryInput,
  SearchResult, AnalyticsResponse, IntegrationRequest, IntegrationResponse,
  AuthRequest, AuthResponse, User
} from '../types';

const API_BASE = '/api/v1';

/**
 * Reads the JWT from `localStorage`.
 * @returns The stored token or `null` when the user is not authenticated.
 */
function getAuthToken(): string | null {
  return localStorage.getItem('token');
}

/**
 * Builds the `Authorization` header object for authenticated requests.
 * Returns an empty object when no token is present.
 */
function getAuthHeaders(): Record<string, string> {
  const token = getAuthToken();
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

/**
 * Unwraps a `fetch` {@link Response} into the expected type `T`.
 * Throws an `Error` with the server's error text on non-2xx status codes.
 * Returns `undefined` for `204 No Content` responses.
 *
 * @template T - Expected response body type.
 * @param response - The raw `fetch` response.
 */
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // Redirect to login on auth failures
    if (response.status === 401 || response.status === 403) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.hash = '#/login';
    }
    const text = await response.text().catch(() => '');
    throw new Error(text || `HTTP ${response.status}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

/**
 * Serialises an object of optional query parameters into a URL query string.
 * `undefined` and `null` values are omitted.
 *
 * @param params - Key-value pairs to encode.
 * @returns A string starting with `"?"` or an empty string when `params` is empty.
 */
function buildQuery(params?: Record<string, any>): string {
  if (!params) return '';
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null);
  if (entries.length === 0) return '';
  return '?' + entries.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&');
}

/**
 * Root API client object.  All domain namespaces are exposed as properties.
 */
export const api = {
  /**
   * Authentication endpoints — no Bearer token required.
   * @category API
   */
  auth: {
    /**
     * Register a new local account.
     * @param request - `{ email, password, name }`
     * @returns JWT + user profile on success.
     */
    register: async (request: AuthRequest): Promise<AuthResponse> => {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    /**
     * Authenticate an existing local account.
     * @param request - `{ email, password }`
     * @returns JWT + user profile on success.
     */
    login: async (request: AuthRequest): Promise<AuthResponse> => {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    /**
     * Authenticate or register via Google OAuth.
     * @param request - `{ googleId, googleToken }`
     * @returns JWT + user profile on success.
     */
    loginWithGoogle: async (request: AuthRequest): Promise<AuthResponse> => {
      const res = await fetch(`${API_BASE}/auth/google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    /**
     * Fetch the currently authenticated user's profile from the backend.
     * @returns The authenticated {@link User}.
     */
    getCurrentUser: async (): Promise<User> => {
      const res = await fetch(`${API_BASE}/users/me`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Clear the JWT and user profile from `localStorage`.
     * Does not call a server-side invalidation endpoint.
     */
    logout: async (): Promise<void> => {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  },

  /**
   * Notes CRUD and lifecycle endpoints.
   * @category API
   */
  notes: {
    /**
     * List notes for the current user with optional filters.
     * @param params - Optional filter parameters.
     * @returns Array of matching {@link Note} objects.
     */
    getAll: async (params?: {
      /** Filter by top-level folder name. */
      folder?: string;
      /** Filter by tag (exact match). */
      tag?: string;
      /** Filter by priority (`"low"` | `"medium"` | `"high"`). */
      priority?: string;
      /** `true` to return only favorites. */
      isFavorite?: boolean;
      /** `true` to return only encrypted notes. */
      isEncrypted?: boolean;
      /** `true` to return only archived notes. */
      isArchived?: boolean;
      /** `true` to return only soft-deleted notes. */
      isDeleted?: boolean;
    }): Promise<Note[]> => {
      const res = await fetch(`${API_BASE}/notes${buildQuery(params)}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Fetch a single note by ID.
     * @param id - Note UUID.
     */
    getById: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Create a new note.
     * @param input - Note fields.
     * @returns The newly created {@link Note}.
     */
    create: async (input: NoteInput): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    /**
     * Update an existing note.
     * @param id - Note UUID.
     * @param input - Fields to update.
     * @returns The updated {@link Note}.
     */
    update: async (id: string, input: NoteInput): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    /**
     * Delete a note.
     * @param id - Note UUID.
     * @param permanent - `false` (default) moves to trash; `true` hard-deletes.
     */
    delete: async (id: string, permanent = false): Promise<void> => {
      const res = await fetch(`${API_BASE}/notes/${id}?permanent=${permanent}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Move a note to the archive.
     * @param id - Note UUID.
     */
    archive: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}/archive`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Restore a note from archive or trash.
     * @param id - Note UUID.
     */
    restore: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}/restore`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Retrieve the full version history of a note.
     * @param id - Note UUID.
     * @returns Array of {@link NoteHistory} entries (oldest first).
     */
    getHistory: async (id: string): Promise<NoteHistory[]> => {
      const res = await fetch(`${API_BASE}/notes/${id}/history`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Import a Markdown file as a new note.
     * @param file - `.md` file selected by the user.
     * @param folder - Destination folder (optional).
     * @param subfolder - Destination sub-folder (optional).
     * @returns The imported {@link Note}.
     */
    importFile: async (file: File, folder?: string, subfolder?: string): Promise<Note> => {
      const formData = new FormData();
      formData.append('file', file);
      if (folder) formData.append('folder', folder);
      if (subfolder) formData.append('subfolder', subfolder);
      const res = await fetch(`${API_BASE}/notes/import`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: formData
      });
      return handleResponse(res);
    }
  },

  /**
   * Todos CRUD and lifecycle endpoints.
   * @category API
   */
  todos: {
    /**
     * List todos with optional filters.
     * @param params - Optional filter parameters.
     */
    getAll: async (params?: {
      /** `true` to return only completed todos. */
      completed?: boolean;
      /** Filter by tag. */
      tag?: string;
      /** Filter by priority. */
      priority?: string;
      /** `true` to return only favorites. */
      isFavorite?: boolean;
      /** `true` to return only archived todos. */
      isArchived?: boolean;
      /** `true` to return only soft-deleted todos. */
      isDeleted?: boolean;
    }): Promise<Todo[]> => {
      const res = await fetch(`${API_BASE}/todos${buildQuery(params)}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Fetch a single todo by ID.
     * @param id - Todo UUID.
     */
    getById: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Create a new todo.
     * @param input - Todo fields.
     */
    create: async (input: TodoInput): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    /**
     * Update an existing todo.
     * @param id - Todo UUID.
     * @param input - Fields to update.
     */
    update: async (id: string, input: TodoInput): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    /**
     * Delete a todo.
     * @param id - Todo UUID.
     * @param permanent - `false` soft-deletes; `true` hard-deletes.
     */
    delete: async (id: string, permanent = false): Promise<void> => {
      const res = await fetch(`${API_BASE}/todos/${id}?permanent=${permanent}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Move a todo to the archive.
     * @param id - Todo UUID.
     */
    archive: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}/archive`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Restore a todo from archive or trash.
     * @param id - Todo UUID.
     */
    restore: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}/restore`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  /**
   * Note template management endpoints.
   * @category API
   */
  templates: {
    /**
     * List all templates, optionally filtered by category.
     * @param category - Category name to filter by.
     */
    getAll: async (category?: string): Promise<NoteTemplate[]> => {
      const res = await fetch(`${API_BASE}/templates${buildQuery({ category })}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Create a new template.
     * @param input - Template fields.
     */
    create: async (input: NoteTemplateInput): Promise<NoteTemplate> => {
      const res = await fetch(`${API_BASE}/templates`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    /**
     * Permanently delete a template.
     * @param id - Template UUID.
     */
    delete: async (id: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/templates/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  /**
   * Full-text search and saved-query endpoints.
   * @category API
   */
  search: {
    /**
     * Execute a full-text search across notes and todos.
     * @param query - Search text.
     * @param type - `"notes"` | `"todos"` | `"all"` (default `"all"`).
     * @param tags - Comma-separated tag filter.
     * @param priority - Priority filter string.
     * @returns Combined {@link SearchResult}.
     */
    search: async (query: string, type?: string, tags?: string, priority?: string): Promise<SearchResult> => {
      const res = await fetch(`${API_BASE}/search${buildQuery({ query, type, tags, priority })}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Retrieve all saved queries for the current user.
     */
    getSavedQueries: async (): Promise<SavedQuery[]> => {
      const res = await fetch(`${API_BASE}/search/queries`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Persist a search query for future use.
     * @param input - Query name, text, and optional filters.
     */
    saveQuery: async (input: SavedQueryInput): Promise<SavedQuery> => {
      const res = await fetch(`${API_BASE}/search/queries`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    /**
     * Delete a saved query.
     * @param id - Query UUID.
     */
    deleteQuery: async (id: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/search/queries/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  /**
   * Usage analytics endpoint.
   * @category API
   */
  analytics: {
    /**
     * Fetch aggregated stats for the given time range.
     * @param timeRange - `"week"` | `"month"` | `"year"` (default `"week"`).
     * @returns {@link AnalyticsResponse} with activity and distribution data.
     */
    get: async (timeRange?: string): Promise<AnalyticsResponse> => {
      const res = await fetch(`${API_BASE}/analytics${buildQuery({ timeRange })}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  /**
   * File attachment upload and download endpoints.
   * @category API
   */
  attachments: {
    /**
     * Upload a single file and associate it with a note or todo.
     * @param file - The file to upload.
     * @param parentId - UUID of the owning note or todo.
     * @param parentType - `"note"` or `"todo"`.
     * @returns The saved attachment record from the backend.
     */
    upload: async (file: File, parentId: string, parentType: string): Promise<any> => {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('parentId', parentId);
      formData.append('parentType', parentType);
      const res = await fetch(`${API_BASE}/attachments/upload`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: formData
      });
      return handleResponse(res);
    },

    /**
     * Upload multiple files in a single multipart request.
     * @param files - Files to upload.
     * @param parentId - UUID of the owning note or todo.
     * @param parentType - `"note"` or `"todo"`.
     * @returns Array of saved attachment records.
     */
    uploadBatch: async (files: File[], parentId: string, parentType: string): Promise<any[]> => {
      const formData = new FormData();
      files.forEach(file => formData.append('files', file));
      formData.append('parentId', parentId);
      formData.append('parentType', parentType);
      const res = await fetch(`${API_BASE}/attachments/upload-batch`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: formData
      });
      return handleResponse(res);
    },

    /**
     * Delete an attachment from the server.
     * @param attachmentId - Attachment UUID.
     */
    delete: async (attachmentId: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/attachments/${attachmentId}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    /**
     * Download an attachment as a `Blob`.
     * @param attachmentId - Attachment UUID.
     * @returns Raw file blob suitable for `URL.createObjectURL`.
     */
    download: async (attachmentId: string): Promise<Blob> => {
      const res = await fetch(`${API_BASE}/attachments/${attachmentId}/download`, {
        headers: getAuthHeaders()
      });
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }
      return res.blob();
    }
  },

  /**
   * Messaging integration endpoints (Telegram, DingTalk, Email).
   * @category API
   */
  integrations: {
    /**
     * Send a message via Telegram bot.
     * @param request - Must include `botToken` and `chatId`.
     */
    sendToTelegram: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/telegram`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    /**
     * Send a message via DingTalk webhook.
     * @param request - Must include `webhook`; `secret` is optional.
     */
    sendToDingtalk: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/dingtalk`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    /**
     * Send a message via SMTP email.
     * @param request - Must include `to`, `subject`, and `message`.
     */
    sendEmail: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    }
  },

  /**
   * User settings endpoints (encrypted on backend).
   * @category API
   */
  settings: {
    /**
     * Get current user's settings (sensitive fields decrypted by backend).
     */
    get: async (): Promise<any> => {
      const res = await fetch(`${API_BASE}/settings`, {
        headers: { ...getAuthHeaders() }
      });
      return handleResponse(res);
    },

    /**
     * Save current user's settings (sensitive fields encrypted by backend).
     */
    save: async (settings: any): Promise<void> => {
      const res = await fetch(`${API_BASE}/settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(settings)
      });
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(text || `HTTP ${res.status}`);
      }
      // Backend returns 200 OK with empty body — no JSON to parse
    }
  },

  /**
   * Tag endpoints — fetch distinct tags across notes and todos.
   * @category API
   */
  tags: {
    /**
     * Get all unique tags for the current user (from notes + todos).
     */
    getAll: async (): Promise<string[]> => {
      const res = await fetch(`${API_BASE}/tags`, {
        headers: { ...getAuthHeaders() }
      });
      return handleResponse(res);
    }
  }
};
