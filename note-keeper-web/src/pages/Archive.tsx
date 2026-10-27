/**
 * @module Archive
 * @category Pages
 * @description Archive page — view and restore archived notes and todos.
 */
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {MarkdownPreview} from '../components/MarkdownPreview';
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
      <Header title="Archive" subtitle={`${notes.length + todos.length} items`} />

      <div className="flex-1 overflow-auto px-5 sm:px-8 lg:px-10 pb-8">
        <div className="mb-10">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-[15px] font-medium text-text">Notes</h3>
            <span className="text-[13px] text-text-secondary">{notes.length}</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {notes.map(note => (
              <div
                key={note.id}
                className="nk-card p-5"
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
                <MarkdownPreview content={note.content || ''} maxLines={3} className="text-text-secondary text-sm mb-4" emptyText="No content" />
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
            <p className="text-text-secondary text-[15px] py-6">No archived notes.</p>
          )}
        </div>

        <div>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-[15px] font-medium text-text">Todos</h3>
            <span className="text-[13px] text-text-secondary">{todos.length}</span>
          </div>
          <div className="max-w-3xl space-y-3">
            {todos.map(todo => (
              <div
                key={todo.id}
                className="nk-card p-5"
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
                  <MarkdownPreview content={todo.description} maxLines={3} className="text-text-secondary mb-3" />
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
            <p className="text-text-secondary text-[15px] py-6">No archived todos.</p>
          )}
        </div>
      </div>
    </PageShell>
  );
};
