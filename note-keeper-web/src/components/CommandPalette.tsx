import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { Note, Todo } from '../types';
import { useFocusTrap } from '../hooks/useFocusTrap';

type PaletteItem = {
  id: string;
  group: string;
  label: string;
  hint?: string;
  icon: string;
  run: () => void;
};

const NAV: { path: string; label: string; icon: string }[] = [
  { path: '/', label: 'Home', icon: 'fa-house' },
  { path: '/notes', label: 'Notes', icon: 'fa-note-sticky' },
  { path: '/todos', label: 'Todos', icon: 'fa-list-check' },
  { path: '/vault', label: 'Vault', icon: 'fa-vault' },
  { path: '/search', label: 'Search page', icon: 'fa-magnifying-glass' },
  { path: '/calendar', label: 'Calendar', icon: 'fa-calendar' },
  { path: '/analytics', label: 'Analytics', icon: 'fa-chart-line' },
  { path: '/favorites', label: 'Favorites', icon: 'fa-star' },
  { path: '/templates', label: 'Templates', icon: 'fa-file-lines' },
  { path: '/archive', label: 'Archive', icon: 'fa-box-archive' },
  { path: '/trash', label: 'Trash', icon: 'fa-trash' },
  { path: '/settings', label: 'Settings', icon: 'fa-gear' },
];

const match = (hay: string, needle: string) =>
  hay.toLowerCase().includes(needle.toLowerCase());

export const CommandPalette: React.FC = () => {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const trapRef = useFocusTrap(open);

  const close = useCallback(() => {
    setOpen(false);
    setQuery('');
    setActive(0);
  }, []);

  useEffect(() => {
    const onOpen = () => setOpen(prev => {
      if (prev) {
        setQuery('');
        setActive(0);
        return false;
      }
      return true;
    });
    window.addEventListener('open-command-palette', onOpen);
    return () => window.removeEventListener('open-command-palette', onOpen);
  }, []);

  useEffect(() => {
    if (!open) return;
    const id = window.setTimeout(() => inputRef.current?.focus(), 20);
    let cancelled = false;
    Promise.all([
      api.notes.getAll({ isArchived: false, isDeleted: false }).catch(() => [] as Note[]),
      api.todos.getAll({ isArchived: false, isDeleted: false }).catch(() => [] as Todo[]),
    ]).then(([n, t]) => {
      if (!cancelled) {
        setNotes(n);
        setTodos(t);
      }
    });
    return () => {
      cancelled = true;
      window.clearTimeout(id);
    };
  }, [open]);

  const items = useMemo<PaletteItem[]>(() => {
    const q = query.trim();
    const commands: PaletteItem[] = [
      {
        id: 'cmd-note',
        group: 'Commands',
        label: 'New note',
        icon: 'fa-plus',
        run: async () => {
          try {
            const note = await api.notes.create({
              title: 'Untitled Note',
              content: '',
              tags: [],
              folder: 'default',
              priority: 'medium',
              isFavorite: false,
              isEncrypted: false,
            });
            navigate(`/notes/${note.id}`);
          } catch { /* keep palette usable offline */ }
        },
      },
      {
        id: 'cmd-todo',
        group: 'Commands',
        label: 'New todo',
        icon: 'fa-check',
        run: async () => {
          try {
            const todo = await api.todos.create({
              title: 'New Todo',
              description: '',
              tags: [],
              priority: 'medium',
              isFavorite: false,
              completed: false,
            });
            navigate(`/todos/${todo.id}`);
          } catch { /* keep palette usable offline */ }
        },
      },
      ...NAV.map(item => ({
        id: `nav-${item.path}`,
        group: 'Go to',
        label: item.label,
        icon: item.icon,
        run: () => navigate(item.path),
      })),
    ].filter(item => !q || match(item.label, q));

    const noteItems: PaletteItem[] = q
      ? notes
          .filter(n => match(n.title, q) || match(n.content, q))
          .slice(0, 8)
          .map(n => ({
            id: `note-${n.id}`,
            group: 'Notes',
            label: n.title || 'Untitled',
            hint: n.folder,
            icon: 'fa-note-sticky',
            run: () => navigate(`/notes/${n.id}`),
          }))
      : [];

    const todoItems: PaletteItem[] = q
      ? todos
          .filter(t => match(t.title, q) || match(t.description || '', q))
          .slice(0, 8)
          .map(t => ({
            id: `todo-${t.id}`,
            group: 'Todos',
            label: t.title || 'Untitled',
            hint: t.completed ? 'Done' : 'Open',
            icon: 'fa-list-check',
            run: () => navigate(`/todos/${t.id}`),
          }))
      : [];

    return [...commands, ...noteItems, ...todoItems];
  }, [query, notes, todos, navigate]);

  useEffect(() => {
    setActive(0);
  }, [query]);

  useEffect(() => {
    const el = listRef.current?.querySelector<HTMLElement>(`[data-idx="${active}"]`);
    el?.scrollIntoView({ block: 'nearest' });
  }, [active]);

  const run = (item: PaletteItem) => {
    close();
    item.run();
  };

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      close();
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive(i => (i + 1) % Math.max(items.length, 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive(i => (i - 1 + Math.max(items.length, 1)) % Math.max(items.length, 1));
    } else if (e.key === 'Enter' && items[active]) {
      e.preventDefault();
      run(items[active]);
    }
  };

  if (!open) return null;

  let lastGroup = '';

  return (
    <div className="fixed inset-0 z-[80] flex items-start justify-center px-4 pt-[12vh]" role="presentation">
      <div className="absolute inset-0 bg-black/50" onClick={close} aria-hidden="true" />
      <div
        ref={trapRef}
        role="dialog"
        aria-modal="true"
        aria-label="Command palette"
        className="relative w-full max-w-xl nk-card shadow-2xl overflow-hidden nk-fade"
        onKeyDown={onKeyDown}
      >
        <div className="flex items-center gap-3 px-4 py-3 border-b border-border">
          <i className="fas fa-search text-text-secondary" aria-hidden="true"></i>
          <input
            ref={inputRef}
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search notes, todos, commands…"
            className="nk-bare-input flex-1 bg-transparent border-0 outline-none text-[15px] text-text placeholder:text-text-secondary"
            aria-label="Command search"
          />
          <span className="nk-kbd">Esc</span>
        </div>
        <div ref={listRef} className="max-h-[min(420px,60vh)] overflow-y-auto py-2">
          {items.length === 0 ? (
            <p className="px-4 py-8 text-center text-text-secondary text-[15px]">Nothing matches.</p>
          ) : (
            items.map((item, idx) => {
              const showGroup = item.group !== lastGroup;
              lastGroup = item.group;
              return (
                <React.Fragment key={item.id}>
                  {showGroup && (
                    <p className="px-4 pt-3 pb-1 text-[11px] font-medium uppercase tracking-wider text-text-secondary">
                      {item.group}
                    </p>
                  )}
                  <button
                    type="button"
                    data-idx={idx}
                    onMouseEnter={() => setActive(idx)}
                    onClick={() => run(item)}
                    className={`w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors duration-[180ms] ${
                      idx === active ? 'bg-hover' : ''
                    }`}
                  >
                    <i className={`fas ${item.icon} w-4 text-text-secondary`} aria-hidden="true"></i>
                    <span className="flex-1 text-[15px] text-text truncate">{item.label}</span>
                    {item.hint && <span className="text-[11px] text-text-secondary shrink-0">{item.hint}</span>}
                  </button>
                </React.Fragment>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export const openCommandPalette = () => {
  window.dispatchEvent(new CustomEvent('open-command-palette'));
};
