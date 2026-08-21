/**
 * In-memory mock data store for MSW dev mocks.
 * All CRUD handlers mutate these arrays so the UI stays consistent
 * within a single dev-session (resets on page reload).
 */

const now = new Date().toISOString();
const yesterday = new Date(Date.now() - 86400000).toISOString();
const lastWeek = new Date(Date.now() - 7 * 86400000).toISOString();
const nextWeek = new Date(Date.now() + 7 * 86400000).toISOString();
const tomorrow = new Date(Date.now() + 86400000).toISOString();

// ── Users ────────────────────────────────────────────────────────────────────

export const mockUser = {
  id: 'mock-user-001',
  email: 'test@example.com',
  name: 'Test User',
  avatarUrl: '',
  provider: 'local' as const,
  isActive: true,
  createdAt: now,
  updatedAt: now,
};

export const MOCK_PASSWORD = 'test123';

// ── Notes ────────────────────────────────────────────────────────────────────

export let notes: any[] = [
  {
    id: 'note-001',
    title: 'Welcome to NoteKeeper',
    content: '# Welcome!\n\nThis is your **personal note-taking app**.\n\n## Features\n- Markdown support\n- Encryption\n- Tags & folders\n- Templates\n\n> Start by editing this note or creating a new one.',
    tags: ['welcome', 'getting-started'],
    folder: 'Personal',
    priority: 'medium',
    isFavorite: true,
    isEncrypted: false,
    isArchived: false,
    isDeleted: false,
    attachments: [],
    ownerId: mockUser.id,
    sharedWith: '[]',
    createdAt: lastWeek,
    updatedAt: yesterday,
  },
  {
    id: 'note-002',
    title: 'Project Roadmap Q3',
    content: '# Q3 Roadmap\n\n## Goals\n1. Launch v2.0\n2. Improve performance\n3. Add mobile support\n\n## Timeline\n| Phase | Start | End |\n|-------|-------|-----|\n| Design | Jul 1 | Jul 15 |\n| Dev | Jul 16 | Aug 20 |\n| QA | Aug 21 | Sep 5 |\n| Release | Sep 6 | Sep 10 |',
    tags: ['work', 'planning'],
    folder: 'Work',
    priority: 'high',
    isFavorite: false,
    isEncrypted: false,
    isArchived: false,
    isDeleted: false,
    attachments: [],
    ownerId: mockUser.id,
    sharedWith: '[]',
    createdAt: lastWeek,
    updatedAt: now,
  },
  {
    id: 'note-003',
    title: 'Meeting Notes — Standup',
    content: '## Standup 2026-08-07\n\n- [x] Reviewed PR #42\n- [ ] Deploy staging build\n- [ ] Update API docs\n\n### Blockers\n- Waiting on design review for settings page',
    tags: ['work', 'meetings'],
    folder: 'Work',
    priority: 'low',
    isFavorite: false,
    isEncrypted: false,
    isArchived: false,
    isDeleted: false,
    attachments: [],
    ownerId: mockUser.id,
    sharedWith: '[]',
    createdAt: yesterday,
    updatedAt: yesterday,
  },
  {
    id: 'note-004',
    title: 'Encrypted Secrets',
    content: 'This note is encrypted with AES-256-GCM.',
    tags: ['private'],
    folder: 'Personal',
    priority: 'medium',
    isFavorite: false,
    isEncrypted: true,
    isArchived: false,
    isDeleted: false,
    attachments: [],
    ownerId: mockUser.id,
    sharedWith: '[]',
    createdAt: lastWeek,
    updatedAt: lastWeek,
  },
  {
    id: 'note-005',
    title: 'Old Draft',
    content: 'This note was archived.',
    tags: [],
    folder: 'Personal',
    priority: 'low',
    isFavorite: false,
    isEncrypted: false,
    isArchived: true,
    isDeleted: false,
    attachments: [],
    ownerId: mockUser.id,
    sharedWith: '[]',
    createdAt: lastWeek,
    updatedAt: lastWeek,
  },
];

// ── Todos ────────────────────────────────────────────────────────────────────

export let todos: any[] = [
  {
    id: 'todo-001',
    title: 'Buy groceries',
    description: 'Milk, eggs, bread, cheese',
    tags: ['personal'],
    priority: 'medium',
    isFavorite: false,
    completed: false,
    dueDate: tomorrow,
    reminder: tomorrow,
    notifiedAt: null,
    location: null,
    schedule: { repeat: 'none' },
    attachments: [],
    isArchived: false,
    isDeleted: false,
    ownerId: mockUser.id,
    sharedWith: '[]',
    lastCompletedAt: null,
    createdAt: yesterday,
    updatedAt: yesterday,
  },
  {
    id: 'todo-002',
    title: 'Daily standup',
    description: 'Team sync at 10:00',
    tags: ['work'],
    priority: 'high',
    isFavorite: true,
    completed: false,
    dueDate: now,
    reminder: now,
    notifiedAt: null,
    location: { lat: 55.7558, lng: 37.6173, address: 'Office, Moscow' },
    schedule: { repeat: 'daily' },
    attachments: [],
    isArchived: false,
    isDeleted: false,
    ownerId: mockUser.id,
    sharedWith: '[]',
    lastCompletedAt: yesterday,
    createdAt: lastWeek,
    updatedAt: yesterday,
  },
  {
    id: 'todo-003',
    title: 'Review pull requests',
    description: 'Check open PRs on Gitea',
    tags: ['work', 'code-review'],
    priority: 'medium',
    isFavorite: false,
    completed: true,
    dueDate: yesterday,
    reminder: yesterday,
    notifiedAt: yesterday,
    location: null,
    schedule: { repeat: 'none' },
    attachments: [],
    isArchived: false,
    isDeleted: false,
    ownerId: mockUser.id,
    sharedWith: '[]',
    lastCompletedAt: null,
    createdAt: lastWeek,
    updatedAt: now,
  },
  {
    id: 'todo-004',
    title: 'Weekly report',
    description: 'Submit weekly status report',
    tags: ['work'],
    priority: 'high',
    isFavorite: false,
    completed: false,
    dueDate: nextWeek,
    reminder: nextWeek,
    notifiedAt: null,
    location: null,
    schedule: { repeat: 'weekly' },
    attachments: [],
    isArchived: false,
    isDeleted: false,
    ownerId: mockUser.id,
    sharedWith: '[]',
    lastCompletedAt: lastWeek,
    createdAt: lastWeek,
    updatedAt: lastWeek,
  },
];

// ── Templates ────────────────────────────────────────────────────────────────

export const templates: any[] = [
  {
    id: 'tmpl-001',
    name: 'Meeting Notes',
    content: '# Meeting Notes\n\n**Date:**\n**Attendees:**\n\n## Agenda\n- [ ]\n\n## Discussion\n\n## Action Items\n- [ ]',
    tags: ['meeting', 'work'],
    category: 'Work',
    createdAt: now,
  },
  {
    id: 'tmpl-002',
    name: 'Daily Journal',
    content: '# Daily Journal\n\n**Date:**\n\n## What I accomplished today\n-\n\n## What I learned\n-\n\n## Plans for tomorrow\n- [ ]',
    tags: ['journal', 'personal'],
    category: 'Personal',
    createdAt: now,
  },
];

// ── Saved queries ────────────────────────────────────────────────────────────

export let savedQueries: any[] = [
  {
    id: 'sq-001',
    name: 'Work tasks',
    query: 'work',
    filters: { type: 'all', tags: ['work'] },
    createdAt: lastWeek,
  },
];

// ── Helpers ──────────────────────────────────────────────────────────────────

let idCounter = 100;

export function generateId(): string {
  return `mock-${++idCounter}-${Date.now().toString(36)}`;
}

export function findNote(id: string) {
  return notes.find(n => n.id === id);
}

export function findTodo(id: string) {
  return todos.find(t => t.id === id);
}

