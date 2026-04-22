import {
  Note, Todo, NoteTemplate, SavedQuery, NoteHistory,
  NoteInput, TodoInput, NoteTemplateInput, SavedQueryInput,
  SearchResult, AnalyticsResponse, IntegrationRequest, IntegrationResponse,
  AuthRequest, AuthResponse, User
} from '../types';

const API_BASE = '/api/v1';

// Get auth token from localStorage
function getAuthToken(): string | null {
  return localStorage.getItem('token');
}

// Add auth header to requests
function getAuthHeaders(): Record<string, string> {
  const token = getAuthToken();
  return token ? { 'Authorization': `Bearer ${token}` } : {};
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new Error(text || `HTTP ${response.status}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

function buildQuery(params?: Record<string, any>): string {
  if (!params) return '';
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null);
  if (entries.length === 0) return '';
  return '?' + entries.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&');
}

export const api = {
  auth: {
    register: async (request: AuthRequest): Promise<AuthResponse> => {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    login: async (request: AuthRequest): Promise<AuthResponse> => {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    loginWithGoogle: async (request: AuthRequest): Promise<AuthResponse> => {
      const res = await fetch(`${API_BASE}/auth/google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    getCurrentUser: async (): Promise<User> => {
      const res = await fetch(`${API_BASE}/users/me`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    logout: async (): Promise<void> => {
      // Clear local storage
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  },

  notes: {
    getAll: async (params?: {
      folder?: string; tag?: string; priority?: string;
      isFavorite?: boolean; isEncrypted?: boolean;
      isArchived?: boolean; isDeleted?: boolean;
    }): Promise<Note[]> => {
      const res = await fetch(`${API_BASE}/notes${buildQuery(params)}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    getById: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    create: async (input: NoteInput): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    update: async (id: string, input: NoteInput): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    delete: async (id: string, permanent = false): Promise<void> => {
      const res = await fetch(`${API_BASE}/notes/${id}?permanent=${permanent}`, { 
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    archive: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}/archive`, { 
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    restore: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}/restore`, { 
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    getHistory: async (id: string): Promise<NoteHistory[]> => {
      const res = await fetch(`${API_BASE}/notes/${id}/history`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

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

  todos: {
    getAll: async (params?: {
      completed?: boolean; tag?: string; priority?: string;
      isFavorite?: boolean; isArchived?: boolean; isDeleted?: boolean;
    }): Promise<Todo[]> => {
      const res = await fetch(`${API_BASE}/todos${buildQuery(params)}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    getById: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    create: async (input: TodoInput): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    update: async (id: string, input: TodoInput): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    delete: async (id: string, permanent = false): Promise<void> => {
      const res = await fetch(`${API_BASE}/todos/${id}?permanent=${permanent}`, { 
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    archive: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}/archive`, { 
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    restore: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}/restore`, { 
        method: 'POST',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  templates: {
    getAll: async (category?: string): Promise<NoteTemplate[]> => {
      const res = await fetch(`${API_BASE}/templates${buildQuery({ category })}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    create: async (input: NoteTemplateInput): Promise<NoteTemplate> => {
      const res = await fetch(`${API_BASE}/templates`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    delete: async (id: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/templates/${id}`, { 
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  search: {
    search: async (query: string, type?: string, tags?: string, priority?: string): Promise<SearchResult> => {
      const res = await fetch(`${API_BASE}/search${buildQuery({ query, type, tags, priority })}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    getSavedQueries: async (): Promise<SavedQuery[]> => {
      const res = await fetch(`${API_BASE}/search/queries`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

    saveQuery: async (input: SavedQueryInput): Promise<SavedQuery> => {
      const res = await fetch(`${API_BASE}/search/queries`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    deleteQuery: async (id: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/search/queries/${id}`, { 
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  analytics: {
    get: async (timeRange?: string): Promise<AnalyticsResponse> => {
      const res = await fetch(`${API_BASE}/analytics${buildQuery({ timeRange })}`, {
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    }
  },

  attachments: {
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

    delete: async (attachmentId: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/attachments/${attachmentId}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      return handleResponse(res);
    },

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

  integrations: {
    sendToTelegram: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/telegram`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    sendToDingtalk: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/dingtalk`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    sendEmail: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    }
  }
};
