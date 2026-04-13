import {
  Note, Todo, NoteTemplate, SavedQuery, NoteHistory,
  NoteInput, TodoInput, NoteTemplateInput, SavedQueryInput,
  SearchResult, AnalyticsResponse, IntegrationRequest, IntegrationResponse
} from '../types';

const API_BASE = '/api/v1';

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
  notes: {
    getAll: async (params?: {
      folder?: string; tag?: string; priority?: string;
      isFavorite?: boolean; isEncrypted?: boolean;
      isArchived?: boolean; isDeleted?: boolean;
    }): Promise<Note[]> => {
      const res = await fetch(`${API_BASE}/notes${buildQuery(params)}`);
      return handleResponse(res);
    },

    getById: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}`);
      return handleResponse(res);
    },

    create: async (input: NoteInput): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    update: async (id: string, input: NoteInput): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    delete: async (id: string, permanent = false): Promise<void> => {
      const res = await fetch(`${API_BASE}/notes/${id}?permanent=${permanent}`, { method: 'DELETE' });
      return handleResponse(res);
    },

    archive: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}/archive`, { method: 'POST' });
      return handleResponse(res);
    },

    restore: async (id: string): Promise<Note> => {
      const res = await fetch(`${API_BASE}/notes/${id}/restore`, { method: 'POST' });
      return handleResponse(res);
    },

    getHistory: async (id: string): Promise<NoteHistory[]> => {
      const res = await fetch(`${API_BASE}/notes/${id}/history`);
      return handleResponse(res);
    },

    importFile: async (file: File, folder?: string, subfolder?: string): Promise<Note> => {
      const formData = new FormData();
      formData.append('file', file);
      if (folder) formData.append('folder', folder);
      if (subfolder) formData.append('subfolder', subfolder);
      const res = await fetch(`${API_BASE}/notes/import`, {
        method: 'POST',
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
      const res = await fetch(`${API_BASE}/todos${buildQuery(params)}`);
      return handleResponse(res);
    },

    getById: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}`);
      return handleResponse(res);
    },

    create: async (input: TodoInput): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    update: async (id: string, input: TodoInput): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    delete: async (id: string, permanent = false): Promise<void> => {
      const res = await fetch(`${API_BASE}/todos/${id}?permanent=${permanent}`, { method: 'DELETE' });
      return handleResponse(res);
    },

    archive: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}/archive`, { method: 'POST' });
      return handleResponse(res);
    },

    restore: async (id: string): Promise<Todo> => {
      const res = await fetch(`${API_BASE}/todos/${id}/restore`, { method: 'POST' });
      return handleResponse(res);
    }
  },

  templates: {
    getAll: async (category?: string): Promise<NoteTemplate[]> => {
      const res = await fetch(`${API_BASE}/templates${buildQuery({ category })}`);
      return handleResponse(res);
    },

    create: async (input: NoteTemplateInput): Promise<NoteTemplate> => {
      const res = await fetch(`${API_BASE}/templates`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    delete: async (id: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/templates/${id}`, { method: 'DELETE' });
      return handleResponse(res);
    }
  },

  search: {
    search: async (query: string, type?: string, tags?: string, priority?: string): Promise<SearchResult> => {
      const res = await fetch(`${API_BASE}/search${buildQuery({ query, type, tags, priority })}`);
      return handleResponse(res);
    },

    getSavedQueries: async (): Promise<SavedQuery[]> => {
      const res = await fetch(`${API_BASE}/search/queries`);
      return handleResponse(res);
    },

    saveQuery: async (input: SavedQueryInput): Promise<SavedQuery> => {
      const res = await fetch(`${API_BASE}/search/queries`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(input)
      });
      return handleResponse(res);
    },

    deleteQuery: async (id: string): Promise<void> => {
      const res = await fetch(`${API_BASE}/search/queries/${id}`, { method: 'DELETE' });
      return handleResponse(res);
    }
  },

  analytics: {
    get: async (timeRange?: string): Promise<AnalyticsResponse> => {
      const res = await fetch(`${API_BASE}/analytics${buildQuery({ timeRange })}`);
      return handleResponse(res);
    }
  },

  integrations: {
    sendToTelegram: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/telegram`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    sendToDingtalk: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/dingtalk`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    },

    sendEmail: async (request: IntegrationRequest): Promise<IntegrationResponse> => {
      const res = await fetch(`${API_BASE}/integrations/email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
      });
      return handleResponse(res);
    }
  }
};
