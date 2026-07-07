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
    { label: 'Total Notes', value: notes.length, icon: 'fa-note-sticky', color: 'bg-blue-500' },
    { label: 'Active Todos', value: todos.filter(t => !t.completed).length, icon: 'fa-list-check', color: 'bg-green-500' },
    { label: 'Favorites', value: notes.filter(n => n.isFavorite).length + todos.filter(t => t.isFavorite).length, icon: 'fa-star', color: 'bg-yellow-500' },
    { label: 'Archived', value: archivedCount, icon: 'fa-box-archive', color: 'bg-purple-500' },
  ];

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header title="Dashboard" />

      <div className="flex-1 overflow-auto p-4 lg:p-8">
        {loading ? (
          <StatCardsSkeleton />
        ) : (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-6 mb-6 lg:mb-8">
            {stats.map((stat, index) => (
              <div key={index} className="bg-surface rounded-xl p-4 lg:p-6 shadow-sm border border-border">
                <div className="flex items-center justify-between mb-3 lg:mb-4">
                  <div className={`${stat.color} w-10 h-10 lg:w-12 lg:h-12 rounded-lg flex items-center justify-center text-white`}>
                    <i className={`fas ${stat.icon} text-lg lg:text-xl`}></i>
                  </div>
                  <span className="text-2xl lg:text-3xl font-bold text-text">{stat.value}</span>
                </div>
                <p className="text-text-secondary font-medium text-sm lg:text-base">{stat.label}</p>
              </div>
            ))}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 lg:gap-6">
          <div className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base lg:text-lg font-bold text-text">Recent Notes</h3>
              <button
                onClick={() => navigate('/notes')}
                className="text-primary hover:text-primary/80 text-sm font-medium"
              >
                View All
              </button>
            </div>
            <div className="space-y-3">
              {loading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="p-4 border border-border rounded-lg animate-pulse">
                    <div className="h-4 w-1/2 bg-hover rounded mb-2"></div>
                    <div className="h-3 w-full bg-hover rounded"></div>
                  </div>
                ))
              ) : recentNotes.length === 0 ? (
                <p className="text-text-secondary text-center py-8">No notes yet</p>
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

          <div className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base lg:text-lg font-bold text-text">Pending Todos</h3>
              <button
                onClick={() => navigate('/todos')}
                className="text-primary hover:text-primary/80 text-sm font-medium"
              >
                View All
              </button>
            </div>
            <div className="space-y-3">
              {loading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="p-4 border border-border rounded-lg animate-pulse">
                    <div className="h-4 w-1/2 bg-hover rounded mb-2"></div>
                    <div className="h-3 w-full bg-hover rounded"></div>
                  </div>
                ))
              ) : pendingTodos.length === 0 ? (
                <p className="text-text-secondary text-center py-8">No pending todos</p>
              ) : (
                pendingTodos.map(todo => (
                  <div
                    key={todo.id}
                    role="button"
                    tabIndex={0}
                    onClick={() => navigate(`/todos/${todo.id}`)}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); navigate(`/todos/${todo.id}`); } }}
                    className="p-4 border border-border rounded-lg hover:border-primary transition-colors cursor-pointer bg-background"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-text text-sm lg:text-base pr-2">{todo.title}</h4>
                      {todo.isFavorite && (
                        <i className="fas fa-star text-yellow-500 text-sm shrink-0" aria-label="Favorite"></i>
                      )}
                    </div>
                    {todo.description && (
                      <p className="text-sm text-text-secondary leading-relaxed line-clamp-2">{todo.description}</p>
                    )}
                    {todo.dueDate && (
                      <p className="text-xs text-text-secondary mt-2">
                        <i className="fas fa-calendar mr-1" aria-hidden="true"></i>
                        Due: {new Date(todo.dueDate).toLocaleDateString()}
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
