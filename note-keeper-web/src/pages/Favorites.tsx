/**
 * @module Favorites
 * @category Pages
 * @description Favorites page — all favorited notes and todos.
 */
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {NoteCard} from '../components/NoteCard';
import {TodoCard} from '../components/TodoCard';
import {CardGridSkeleton, TodoListSkeleton} from '../components/LoadingSkeleton';
import {api} from '../utils/api';
import {Note, Todo} from '../types';

export const Favorites: React.FC = () => {
  const navigate = useNavigate();
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const [n, t] = await Promise.all([
          api.notes.getAll({ isFavorite: true, isArchived: false, isDeleted: false }),
          api.todos.getAll({ isFavorite: true, isArchived: false, isDeleted: false })
        ]);
        setNotes(n);
        setTodos(t);
      } catch (err) {
        setError((err as any)?.message || 'Failed to load favorites');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header title="Favorites" />

      <div className="flex-1 overflow-auto p-4 lg:p-8">
        <div className="mb-8">
          <h3 className="text-xl font-bold text-text mb-4">Favorite Notes</h3>
          {loading ? (
            <CardGridSkeleton count={3} />
          ) : notes.length === 0 ? (
            <p className="text-text-secondary text-center py-8">No favorite notes</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
              {notes.map(note => (
                <NoteCard
                  key={note.id}
                  note={note}
                  variant="readonly"
                  onClick={() => navigate(`/notes/${note.id}`)}
                />
              ))}
            </div>
          )}
        </div>

        <div>
          <h3 className="text-xl font-bold text-text mb-4">Favorite Todos</h3>
          {loading ? (
            <TodoListSkeleton count={2} />
          ) : todos.length === 0 ? (
            <p className="text-text-secondary text-center py-8">No favorite todos</p>
          ) : (
            <div className="max-w-4xl space-y-4">
              {todos.map(todo => (
                <TodoCard
                  key={todo.id}
                  todo={todo}
                  variant="readonly"
                  onNavigate={() => navigate(`/todos/${todo.id}`)}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </PageShell>
  );
};
