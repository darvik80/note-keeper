export interface Attachment {
  id: string;
  name: string;
  size: number;
  type: string;
  url: string;
  uploadedAt: Date;
}

export interface Note {
  id: string;
  title: string;
  content: string;
  tags: string[];
  folder: string;
  subfolder?: string;
  priority: 'low' | 'medium' | 'high';
  isFavorite: boolean;
  isEncrypted: boolean;
  reminder?: Date;
  attachments: Attachment[];
  isArchived: boolean;
  isDeleted: boolean;
  deletedAt?: Date;
  history: NoteHistory[];
  templateId?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface NoteHistory {
  id: string;
  content: string;
  timestamp: Date;
  action: 'created' | 'edited' | 'restored';
}

export interface Todo {
  id: string;
  title: string;
  description: string;
  tags: string[];
  priority: 'low' | 'medium' | 'high';
  isFavorite: boolean;
  completed: boolean;
  dueDate?: Date;
  reminder?: Date;
  location?: {
    lat: number;
    lng: number;
    address: string;
  };
  schedule?: {
    repeat: 'none' | 'daily' | 'weekly' | 'monthly';
    endDate?: Date;
  };
  attachments: Attachment[];
  isArchived: boolean;
  isDeleted: boolean;
  deletedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
}

export interface NoteTemplate {
  id: string;
  name: string;
  content: string;
  tags: string[];
  category: string;
  createdAt: Date;
}

export interface SavedQuery {
  id: string;
  name: string;
  query: string;
  filters: {
    type?: 'notes' | 'todos' | 'all';
    tags?: string[];
    priority?: 'low' | 'medium' | 'high';
    folder?: string;
  };
  createdAt: Date;
}

export interface Settings {
  telegram: {
    enabled: boolean;
    botToken: string;
    chatId: string;
  };
  dingtalk: {
    enabled: boolean;
    webhook: string;
    secret: string;
  };
  email: {
    enabled: boolean;
    smtp: string;
    port: number;
    username: string;
    password: string;
    from: string;
    to: string;
  };
  theme: string;
  shortcuts: {
    newNote: string;
    newTodo: string;
    search: string;
    toggleSidebar: string;
  };
}

export interface FolderStructure {
  name: string;
  path: string;
  subfolders: FolderStructure[];
}

export type Theme = 'light' | 'dark' | 'green' | 'cyan' | 'blue' | 'purple' | 'darcula' | 'rose' | 'amber' | 'teal' | 'indigo' | 'slate';

// --- API DTOs ---

export interface NoteInput {
  title: string;
  content?: string;
  tags?: string[];
  folder?: string;
  subfolder?: string;
  priority?: string;
  isFavorite?: boolean;
  isEncrypted?: boolean;
  reminder?: string;
  templateId?: string;
}

export interface TodoInput {
  title: string;
  description?: string;
  completed?: boolean;
  tags?: string[];
  priority?: string;
  isFavorite?: boolean;
  dueDate?: string;
  reminder?: string;
  location?: { lat: number; lng: number; address: string };
  schedule?: { repeat: string; endDate?: string };
}

export interface NoteTemplateInput {
  name: string;
  content: string;
  tags?: string[];
  category: string;
}

export interface SavedQueryInput {
  name: string;
  query: string;
  filters?: {
    type?: string;
    tags?: string[];
    priority?: string;
    folder?: string;
  };
}

export interface SearchResult {
  notes: Note[];
  todos: Todo[];
}

export interface AnalyticsResponse {
  notesCreated: number;
  todosCreated: number;
  todosCompleted: number;
  completionRate: number;
  topTags: { tag: string; count: number }[];
  priorityDistribution: Record<string, number>;
  dailyActivity: number[];
}

export interface IntegrationRequest {
  message: string;
  subject?: string;
  botToken?: string;
  chatId?: string;
  webhook?: string;
  secret?: string;
  to?: string;
}

export interface IntegrationResponse {
  success: boolean;
  message: string;
}

export interface User {
  id: string;
  email: string;
  name: string;
  avatarUrl?: string;
  provider: 'local' | 'google';
  googleId?: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface AuthRequest {
  email?: string;
  password?: string;
  name?: string;
  googleId?: string;
  googleToken?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}
