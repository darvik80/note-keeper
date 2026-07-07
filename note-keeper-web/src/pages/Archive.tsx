/**
 * @module Archive
 * @category Pages
 * @description Archive page — view and restore archived notes and todos.
 */
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {api} from '../utils/api';
import {Note, Todo} from '../types';

/** Archive page listing all archived notes and todos with restore and delete actions. */
export const Archive: React.FC = () => {
  const navigate = useNavigate();
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadArchived();
  }, []);

  const loadArchived = async () => {
    try {
      const [n, t] = await Promise.all([
        api.notes.getAll({ isArchived: true, isDeleted: false }),
        api.todos.getAll({ isArchived: true, isDeleted: false })
      ]);
      setNotes(n);
      setTodos(t);
    } catch (err) {
      setError((err as any)?.message || 'Failed to load archived items');
    }
  };

  const unarchiveNote = async (id: string) => {
    try {
      await api.notes.restore(id);
      setNotes(prev => prev.filter(n => n.id !== id));
    } catch (err) {
      setError((err as any)?.message || 'Failed to unarchive note');
    }
  };

  const unarchiveTodo = async (id: string) => {
    try {
      await api.todos.restore(id);
      setTodos(prev => prev.filter(t => t.id !== id));
    } catch (err) {
      setError((err as any)?.message || 'Failed to unarchive todo');
    }
  };

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header title="Archive" />

      <div className="flex-1 overflow-auto p-4 lg:p-8">
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-xl font-bold text-text">Archived Notes</h3>
            <span className="text-sm text-text-secondary">{notes.length} items</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
            {notes.map(note => (
              <div
                key={note.id}
                className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border hover:shadow-md transition-shadow"
              >
                <div className="flex items-start justify-between mb-3">
                  <h4 className="font-bold text-text text-base lg:text-lg flex-1 pr-2">{note.title}</h4>
                  <button
                    onClick={() => unarchiveNote(note.id)}
                    className="text-primary hover:text-primary/80 p-1"
                    title="Unarchive"
                    aria-label="Unarchive note"
                  >
                    <i className="fas fa-box-open"></i>
                  </button>
                </div>
                <p className="text-text-secondary text-sm mb-4 line-clamp-3">{note.content || 'No content'}</p>
                <div className="flex flex-wrap gap-2">
                  {note.tags.slice(0, 3).map(tag => (
                    <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                      #{tag}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
          {notes.length === 0 && (
            <p className="text-text-secondary text-center py-8">No archived notes</p>
          )}
        </div>

        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-xl font-bold text-text">Archived Todos</h3>
            <span className="text-sm text-text-secondary">{todos.length} items</span>
          </div>
          <div className="max-w-4xl space-y-4">
            {todos.map(todo => (
              <div
                key={todo.id}
                className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border hover:shadow-md transition-shadow"
              >
                <div className="flex items-start justify-between mb-2">
                  <h4 className="font-bold text-text text-base lg:text-lg flex-1 pr-2">{todo.title}</h4>
                  <button
                    onClick={() => unarchiveTodo(todo.id)}
                    className="text-primary hover:text-primary/80 p-1"
                    title="Unarchive"
                    aria-label="Unarchive todo"
                  >
                    <i className="fas fa-box-open"></i>
                  </button>
                </div>
                {todo.description && (
                  <p className="text-text-secondary mb-3">{todo.description}</p>
                )}
                <div className="flex flex-wrap gap-2">
                  {todo.tags.map(tag => (
                    <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                      #{tag}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
          {todos.length === 0 && (
            <p className="text-text-secondary text-center py-8">No archived todos</p>
          )}
        </div>
      </div>
    </PageShell>
  );
};
