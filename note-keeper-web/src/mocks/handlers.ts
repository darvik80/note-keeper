/**
 * MSW request handlers — mock every `/api/v1/*` endpoint used by the frontend.
 *
 * In-memory state is kept in `data.ts` so CRUD mutations are reflected
 * immediately in subsequent GET requests within the same browser session.
 */
import { http, HttpResponse } from 'msw';
import {
  mockUser, MOCK_PASSWORD,
  notes, todos, templates, savedQueries,
  generateId, findNote, findTodo,
} from './data';

const API = '/api/v1';

// ── Auth helpers ─────────────────────────────────────────────────────────────

function authUser(req: Request) {
  const auth = req.headers.get('authorization') || '';
  if (!auth.startsWith('Bearer ')) return null;
  return auth === `Bearer mock-jwt-token` ? mockUser : null;
}

// ── Auth ─────────────────────────────────────────────────────────────────────

export const handlers = [
  // POST /auth/login
  http.post(`${API}/auth/login`, async ({ request }) => {
    const body = await request.json() as any;
    if (body.email === mockUser.email && body.password === MOCK_PASSWORD) {
      return HttpResponse.json({ token: 'mock-jwt-token', user: mockUser });
    }
    return HttpResponse.json(
      { message: 'Invalid email or password' },
      { status: 401 },
    );
  }),

  // POST /auth/register
  http.post(`${API}/auth/register`, async ({ request }) => {
    const body = await request.json() as any;
    const user = { ...mockUser, email: body.email || mockUser.email, name: body.name || mockUser.name };
    return HttpResponse.json({ token: 'mock-jwt-token', user }, { status: 201 });
  }),

  // POST /auth/google
  http.post(`${API}/auth/google`, async () => {
    return HttpResponse.json({ token: 'mock-jwt-token', user: mockUser });
  }),

  // GET /users/me
  http.get(`${API}/users/me`, ({ request }) => {
    const user = authUser(request);
    if (!user) return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    return HttpResponse.json(user);
  }),

  // ── Notes ────────────────────────────────────────────────────────────────────

  // GET /notes
  http.get(`${API}/notes`, ({ request }) => {
    const url = new URL(request.url);
    let result = notes.filter(n => !n.isDeleted);

    const folder = url.searchParams.get('folder');
    const tag = url.searchParams.get('tag');
    const priority = url.searchParams.get('priority');
    const isFavorite = url.searchParams.get('isFavorite');
    const isArchived = url.searchParams.get('isArchived');
    const isDeleted = url.searchParams.get('isDeleted');
    const isEncrypted = url.searchParams.get('isEncrypted');

    if (folder) result = result.filter(n => n.folder === folder);
    if (tag) result = result.filter(n => n.tags.includes(tag));
    if (priority) result = result.filter(n => n.priority === priority);
    if (isFavorite === 'true') result = result.filter(n => n.isFavorite);
    if (isArchived === 'true') result = result.filter(n => n.isArchived);
    else if (isArchived !== 'true') result = result.filter(n => !n.isArchived);
    if (isDeleted === 'true') result = notes.filter(n => n.isDeleted);
    else if (!isDeleted) result = result.filter(n => !n.isDeleted);
    if (isEncrypted === 'true') result = result.filter(n => n.isEncrypted);

    return HttpResponse.json(result);
  }),

  // GET /notes/:id
  http.get(`${API}/notes/:id`, ({ params }) => {
    const note = findNote(params.id as string);
    if (!note) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    return HttpResponse.json(note);
  }),

  // POST /notes
  http.post(`${API}/notes`, async ({ request }) => {
    const body = await request.json() as any;
    const now = new Date().toISOString();
    const note = {
      id: generateId(),
      title: body.title || 'Untitled',
      content: body.content || '',
      tags: body.tags || [],
      folder: body.folder || '',
      priority: body.priority || 'medium',
      isFavorite: body.isFavorite || false,
      isEncrypted: body.isEncrypted || false,
      isArchived: false,
      isDeleted: false,
      attachments: [],
      ownerId: mockUser.id,
      sharedWith: '[]',
      createdAt: now,
      updatedAt: now,
    };
    notes.unshift(note);
    return HttpResponse.json(note, { status: 201 });
  }),

  // PUT /notes/:id
  http.put(`${API}/notes/:id`, async ({ params, request }) => {
    const note = findNote(params.id as string);
    if (!note) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    const body = await request.json() as any;
    Object.assign(note, body, { updatedAt: new Date().toISOString() });
    return HttpResponse.json(note);
  }),

  // DELETE /notes/:id
  http.delete(`${API}/notes/:id`, ({ params, request }) => {
    const note = findNote(params.id as string);
    if (!note) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    const url = new URL(request.url);
    if (url.searchParams.get('permanent') === 'true') {
      const idx = notes.indexOf(note);
      if (idx >= 0) notes.splice(idx, 1);
    } else {
      note.isDeleted = true;
      note.updatedAt = new Date().toISOString();
    }
    return new HttpResponse(null, { status: 204 });
  }),

  // POST /notes/:id/archive
  http.post(`${API}/notes/:id/archive`, ({ params }) => {
    const note = findNote(params.id as string);
    if (!note) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    note.isArchived = true;
    note.updatedAt = new Date().toISOString();
    return HttpResponse.json(note);
  }),

  // POST /notes/:id/restore
  http.post(`${API}/notes/:id/restore`, ({ params }) => {
    const note = findNote(params.id as string);
    if (!note) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    note.isArchived = false;
    note.isDeleted = false;
    note.updatedAt = new Date().toISOString();
    return HttpResponse.json(note);
  }),

  // GET /notes/:id/history
  http.get(`${API}/notes/:id/history`, ({ params }) => {
    const note = findNote(params.id as string);
    if (!note) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    return HttpResponse.json([
      { id: generateId(), content: note.content, action: 'created', timestamp: note.createdAt },
    ]);
  }),

  // POST /notes/import
  http.post(`${API}/notes/import`, async ({ request }) => {
    const formData = await request.formData();
    const file = formData.get('file') as File;
    const content = file ? await file.text() : '';
    const now = new Date().toISOString();
    const note = {
      id: generateId(),
      title: file?.name || 'Imported',
      content,
      tags: [],
      folder: formData.get('folder') as string || '',
      priority: 'medium',
      isFavorite: false,
      isEncrypted: false,
      isArchived: false,
      isDeleted: false,
      attachments: [],
      ownerId: mockUser.id,
      sharedWith: '[]',
      createdAt: now,
      updatedAt: now,
    };
    notes.unshift(note);
    return HttpResponse.json(note, { status: 201 });
  }),

  // ── Todos ────────────────────────────────────────────────────────────────────

  // GET /todos
  http.get(`${API}/todos`, ({ request }) => {
    const url = new URL(request.url);
    let result = [...todos];

    const completed = url.searchParams.get('completed');
    const tag = url.searchParams.get('tag');
    const priority = url.searchParams.get('priority');
    const isFavorite = url.searchParams.get('isFavorite');
    const isArchived = url.searchParams.get('isArchived');
    const isDeleted = url.searchParams.get('isDeleted');

    if (completed === 'true') result = result.filter(t => t.completed);
    else if (completed === 'false') result = result.filter(t => !t.completed);
    if (tag) result = result.filter(t => t.tags.includes(tag));
    if (priority) result = result.filter(t => t.priority === priority);
    if (isFavorite === 'true') result = result.filter(t => t.isFavorite);
    if (isArchived === 'true') result = result.filter(t => t.isArchived);
    else result = result.filter(t => !t.isArchived);
    if (isDeleted === 'true') result = result.filter(t => t.isDeleted);
    else result = result.filter(t => !t.isDeleted);

    return HttpResponse.json(result);
  }),

  // GET /todos/shared-with-me
  http.get(`${API}/todos/shared-with-me`, () => {
    return HttpResponse.json([]);
  }),

  // GET /notes/shared-with-me
  http.get(`${API}/notes/shared-with-me`, () => {
    return HttpResponse.json([]);
  }),

  // GET /todos/:id
  http.get(`${API}/todos/:id`, ({ params }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    return HttpResponse.json(todo);
  }),

  // POST /todos
  http.post(`${API}/todos`, async ({ request }) => {
    const body = await request.json() as any;
    const now = new Date().toISOString();
    const todo = {
      id: generateId(),
      title: body.title || 'New Todo',
      description: body.description || '',
      tags: body.tags || [],
      priority: body.priority || 'medium',
      isFavorite: body.isFavorite || false,
      completed: body.completed || false,
      dueDate: body.dueDate || null,
      reminder: body.reminder || null,
      notifiedAt: null,
      location: body.location || null,
      schedule: body.schedule || { repeat: 'none' },
      attachments: [],
      isArchived: false,
      isDeleted: false,
      ownerId: mockUser.id,
      sharedWith: '[]',
      lastCompletedAt: null,
      createdAt: now,
      updatedAt: now,
    };
    todos.unshift(todo);
    return HttpResponse.json(todo, { status: 201 });
  }),

  // PUT /todos/:id
  http.put(`${API}/todos/:id`, async ({ params, request }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    const body = await request.json() as any;
    Object.assign(todo, body, { updatedAt: new Date().toISOString() });
    return HttpResponse.json(todo);
  }),

  // DELETE /todos/:id
  http.delete(`${API}/todos/:id`, ({ params, request }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    const url = new URL(request.url);
    if (url.searchParams.get('permanent') === 'true') {
      const idx = todos.indexOf(todo);
      if (idx >= 0) todos.splice(idx, 1);
    } else {
      todo.isDeleted = true;
      todo.updatedAt = new Date().toISOString();
    }
    return new HttpResponse(null, { status: 204 });
  }),

  // POST /todos/:id/archive
  http.post(`${API}/todos/:id/archive`, ({ params }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    todo.isArchived = true;
    todo.updatedAt = new Date().toISOString();
    return HttpResponse.json(todo);
  }),

  // POST /todos/:id/restore
  http.post(`${API}/todos/:id/restore`, ({ params }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    todo.isArchived = false;
    todo.isDeleted = false;
    todo.updatedAt = new Date().toISOString();
    return HttpResponse.json(todo);
  }),

  // POST /todos/:id/toggle-complete
  http.post(`${API}/todos/:id/toggle-complete`, ({ params }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });

    const isRecurring = todo.schedule?.repeat && todo.schedule.repeat !== 'none';

    if (isRecurring && !todo.completed) {
      // Complete recurring: advance dates, reset completed
      const now = new Date();
      const repeat = todo.schedule.repeat;
      const advance = (dateStr: string | null) => {
        if (!dateStr) return null;
        const d = new Date(dateStr);
        if (repeat === 'daily') d.setDate(d.getDate() + 1);
        else if (repeat === 'weekly') d.setDate(d.getDate() + 7);
        else if (repeat === 'monthly') d.setMonth(d.getMonth() + 1);
        return d.toISOString();
      };
      todo.lastCompletedAt = now.toISOString();
      todo.reminder = advance(todo.reminder);
      todo.dueDate = advance(todo.dueDate);
      todo.completed = false;
      todo.notifiedAt = null;
      todo.updatedAt = now.toISOString();
    } else {
      todo.completed = !todo.completed;
      todo.updatedAt = new Date().toISOString();
    }

    return HttpResponse.json(todo);
  }),

  // GET /todos/:id/completions
  http.get(`${API}/todos/:id/completions`, ({ params }) => {
    const todo = findTodo(params.id as string);
    if (!todo) return HttpResponse.json({ message: 'Not found' }, { status: 404 });
    return HttpResponse.json([]);
  }),

  // ── Templates ────────────────────────────────────────────────────────────────

  // GET /templates
  http.get(`${API}/templates`, () => {
    return HttpResponse.json(templates);
  }),

  // POST /templates
  http.post(`${API}/templates`, async ({ request }) => {
    const body = await request.json() as any;
    const tmpl = {
      id: generateId(),
      name: body.name || 'Untitled Template',
      content: body.content || '',
      tags: body.tags || [],
      category: body.category || 'Personal',
      createdAt: new Date().toISOString(),
    };
    templates.unshift(tmpl);
    return HttpResponse.json(tmpl, { status: 201 });
  }),

  // DELETE /templates/:id
  http.delete(`${API}/templates/:id`, ({ params }) => {
    const idx = templates.findIndex(t => t.id === params.id);
    if (idx >= 0) templates.splice(idx, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  // ── Search ───────────────────────────────────────────────────────────────────

  // GET /search
  http.get(`${API}/search`, ({ request }) => {
    const url = new URL(request.url);
    const query = (url.searchParams.get('query') || '').toLowerCase();
    const type = url.searchParams.get('type') || 'all';

    const matchNotes = type !== 'todos'
      ? notes.filter(n => !n.isDeleted && (
          n.title.toLowerCase().includes(query) ||
          n.content.toLowerCase().includes(query) ||
          n.tags.some((t: string) => t.toLowerCase().includes(query))
        ))
      : [];

    const matchTodos = type !== 'notes'
      ? todos.filter(t => !t.isDeleted && (
          t.title.toLowerCase().includes(query) ||
          (t.description || '').toLowerCase().includes(query) ||
          t.tags.some((tag: string) => tag.toLowerCase().includes(query))
        ))
      : [];

    return HttpResponse.json({ notes: matchNotes, todos: matchTodos });
  }),

  // GET /search/queries
  http.get(`${API}/search/queries`, () => {
    return HttpResponse.json(savedQueries);
  }),

  // POST /search/queries
  http.post(`${API}/search/queries`, async ({ request }) => {
    const body = await request.json() as any;
    const q = {
      id: generateId(),
      name: body.name || 'Saved query',
      query: body.query || '',
      filters: body.filters || {},
      createdAt: new Date().toISOString(),
    };
    savedQueries.unshift(q);
    return HttpResponse.json(q, { status: 201 });
  }),

  // DELETE /search/queries/:id
  http.delete(`${API}/search/queries/:id`, ({ params }) => {
    const idx = savedQueries.findIndex(q => q.id === params.id);
    if (idx >= 0) savedQueries.splice(idx, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  // ── Analytics ────────────────────────────────────────────────────────────────

  http.get(`${API}/analytics`, () => {
    return HttpResponse.json({
      notesCreated: notes.length,
      todosCreated: todos.length,
      todosCompleted: todos.filter(t => t.completed).length,
      completionRate: todos.length > 0
        ? todos.filter(t => t.completed).length / todos.length
        : 0,
      topTags: [
        { tag: 'work', count: 4 },
        { tag: 'personal', count: 3 },
        { tag: 'planning', count: 1 },
      ],
      priorityDistribution: { low: 2, medium: 3, high: 2 },
      dailyActivity: [1, 3, 2, 5, 4, 2, 1],
    });
  }),

  // ── Attachments ──────────────────────────────────────────────────────────────

  http.post(`${API}/attachments/upload`, async () => {
    return HttpResponse.json({
      id: generateId(),
      name: 'mock-file.txt',
      size: 1024,
      type: 'text/plain',
      url: '#',
      uploadedAt: new Date().toISOString(),
    }, { status: 201 });
  }),

  http.post(`${API}/attachments/upload-batch`, async () => {
    return HttpResponse.json([], { status: 201 });
  }),

  http.delete(`${API}/attachments/:id`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  http.get(`${API}/attachments/:id/download`, () => {
    return new HttpResponse('mock file content', {
      headers: { 'Content-Type': 'application/octet-stream' },
    });
  }),

  // ── Integrations ─────────────────────────────────────────────────────────────

  http.post(`${API}/integrations/telegram`, () => {
    return HttpResponse.json({ success: true, message: 'Telegram message sent (mock)' });
  }),

  http.post(`${API}/integrations/telegram/test-todo`, () => {
    return HttpResponse.json({ success: true, message: 'Test reminder sent (mock)' });
  }),

  http.post(`${API}/integrations/dingtalk`, () => {
    return HttpResponse.json({ success: true, message: 'DingTalk message sent (mock)' });
  }),

  http.post(`${API}/integrations/email`, () => {
    return HttpResponse.json({ success: true, message: 'Email sent (mock)' });
  }),

  // ── Settings ─────────────────────────────────────────────────────────────────

  http.get(`${API}/settings`, () => {
    return HttpResponse.json({
      telegram: { enabled: false, botToken: '', chatId: '' },
      dingtalk: { enabled: false, webhook: '', secret: '' },
      email: { enabled: false, smtp: '', port: 587, username: '', password: '', from: '', to: '' },
    });
  }),

  http.post(`${API}/settings`, () => {
    return new HttpResponse(null, { status: 200 });
  }),

  // ── Tags ─────────────────────────────────────────────────────────────────────

  http.get(`${API}/tags`, () => {
    const tagSet = new Set<string>();
    notes.forEach(n => n.tags.forEach((t: string) => tagSet.add(t)));
    todos.forEach(t => t.tags.forEach((tag: string) => tagSet.add(tag)));
    return HttpResponse.json([...tagSet]);
  }),
];

