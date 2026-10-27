/**
 * @module Dashboard
 * @category Pages
 * @description Dashboard page — overview with stats, recent notes, and recent todos.
 */
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {NoteCard} from '../components/NoteCard';
import {MarkdownPreview} from '../components/MarkdownPreview';
import {StatCardsSkeleton} from '../components/LoadingSkeleton';
import {api} from '../utils/api';
import {Note, Todo} from '../types';

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [archivedCount, setArchivedCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const [n, t] = await Promise.all([
          api.notes.getAll({ isDeleted: false }),
          api.todos.getAll({ isDeleted: false })
        ]);
        setNotes(n.filter(x => !x.isArchived));
        setTodos(t.filter(x => !x.isArchived));
        setArchivedCount(n.filter(x => x.isArchived).length + t.filter(x => x.isArchived).length);
      } catch (err) {
        setError((err as any)?.message || 'Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const recentNotes = notes.slice(0, 5);
  const pendingTodos = todos.filter(t => !t.completed).slice(0, 5);

  const stats = [
    { label: 'Notes', value: notes.length, path: '/notes' },
    { label: 'Open tasks', value: todos.filter(t => !t.completed).length, path: '/todos' },
    { label: 'Favorites', value: notes.filter(n => n.isFavorite).length + todos.filter(t => t.isFavorite).length, path: '/favorites' },
    { label: 'Archived', value: archivedCount, path: '/archive' },
  ];

  const hour = new Date().getHours();
  const hello = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  let firstName = '';
  try {
    firstName = JSON.parse(localStorage.getItem('user') || '{}').name?.split(' ')[0] || '';
  } catch { /* ignore */ }

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header
        title={firstName ? `${hello}, ${firstName}` : hello}
        subtitle="What you have been working on"
        actions={
          <div className="flex items-center gap-2">
            <button type="button" onClick={() => navigate('/notes')} className="nk-btn nk-btn-ghost">
              Notes
            </button>
            <button type="button" onClick={() => navigate('/todos')} className="nk-btn nk-btn-primary">
              Todos
            </button>
          </div>
        }
      />

      <div className="flex-1 overflow-auto px-5 sm:px-8 lg:px-10 pb-8">
        {loading ? (
          <StatCardsSkeleton />
        ) : (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 lg:gap-4 mb-8">
            {stats.map((stat) => (
              <button
                key={stat.path}
                type="button"
                onClick={() => navigate(stat.path)}
                className="nk-card nk-card-hover p-5 text-left"
              >
                <p className="text-3xl font-bold tracking-tight text-text">{stat.value}</p>
                <p className="text-[13px] text-text-secondary mt-1">{stat.label}</p>
              </button>
            ))}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 lg:gap-10">
          <div>
            <div className="flex items-center justify-between mb-3 px-1">
              <h3 className="text-[15px] font-medium text-text">Recent notes</h3>
              <button
                onClick={() => navigate('/notes')}
                className="text-secondary hover:opacity-80 text-[13px]"
              >
                View all
              </button>
            </div>
            <div className="space-y-1">
              {loading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="p-4 rounded-[22px] animate-pulse">
                    <div className="h-4 w-1/2 bg-hover rounded mb-2"></div>
                    <div className="h-3 w-full bg-hover rounded"></div>
                  </div>
                ))
              ) : recentNotes.length === 0 ? (
                <p className="text-text-secondary text-[15px] px-1 py-6">No notes yet.</p>
              ) : (
                recentNotes.map(note => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    variant="compact"
                    onClick={() => navigate(`/notes/${note.id}`)}
                  />
                ))
              )}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-3 px-1">
              <h3 className="text-[15px] font-medium text-text">Pending todos</h3>
              <button
                onClick={() => navigate('/todos')}
                className="text-secondary hover:opacity-80 text-[13px]"
              >
                View all
              </button>
            </div>
            <div className="space-y-1">
              {loading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="p-4 rounded-[22px] animate-pulse">
                    <div className="h-4 w-1/2 bg-hover rounded mb-2"></div>
                    <div className="h-3 w-full bg-hover rounded"></div>
                  </div>
                ))
              ) : pendingTodos.length === 0 ? (
                <p className="text-text-secondary text-[15px] px-1 py-6">Nothing pending.</p>
              ) : (
                pendingTodos.map(todo => (
                  <div
                    key={todo.id}
                    role="button"
                    tabIndex={0}
                    onClick={() => navigate(`/todos/${todo.id}`)}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/todos/${todo.id}`); } }}
                    className="p-4 rounded-[22px] hover:bg-hover transition-colors duration-[180ms] cursor-pointer"
                  >
                    <div className="flex items-start justify-between mb-1">
                      <h4 className="font-semibold text-text text-[15px] pr-2">{todo.title}</h4>
                      {todo.isFavorite && (
                        <i className="fas fa-star text-primary text-xs shrink-0" aria-label="Favorite"></i>
                      )}
                    </div>
                    {todo.description && (
                      <MarkdownPreview content={todo.description} maxLines={3} className="text-[15px] text-text-secondary" />
                    )}
                    {todo.dueDate && (
                      <p className="text-[13px] text-text-secondary mt-2">
                        {new Date(todo.dueDate).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
};
