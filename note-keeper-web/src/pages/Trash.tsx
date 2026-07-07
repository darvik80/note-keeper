/**
 * @module Trash
 * @category Pages
 * @description Trash page — view, restore, and permanently delete soft-deleted items.
 */
import React, { useEffect, useState } from 'react';
import { Header } from '../components/Header';
import { PageShell } from '../components/PageShell';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { useToast } from '../contexts/ToastContext';
import { api } from '../utils/api';
import { Note, Todo } from '../types';

interface ConfirmState {
  title: string;
  message: string;
  onConfirm: () => Promise<void>;
}

export const Trash: React.FC = () => {
  const { toast } = useToast();
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<ConfirmState | null>(null);
  const [confirmLoading, setConfirmLoading] = useState(false);

  useEffect(() => {
    loadDeleted();
  }, []);

  const loadDeleted = async () => {
    try {
      const [n, t] = await Promise.all([
        api.notes.getAll({ isDeleted: true }),
        api.todos.getAll({ isDeleted: true }),
      ]);
      setNotes(n);
      setTodos(t);
    } catch (err) {
      setError((err as any)?.message || 'Failed to load deleted items');
    }
  };

  const restoreNote = async (id: string) => {
    try {
      await api.notes.restore(id);
      setNotes(prev => prev.filter(n => n.id !== id));
      toast.success('Note restored');
    } catch (err) {
      setError((err as any)?.message || 'Failed to restore note');
    }
  };

  const restoreTodo = async (id: string) => {
    try {
      await api.todos.restore(id);
      setTodos(prev => prev.filter(t => t.id !== id));
      toast.success('Todo restored');
    } catch (err) {
      setError((err as any)?.message || 'Failed to restore todo');
    }
  };

  const runConfirm = async () => {
    if (!confirm) return;
    setConfirmLoading(true);
    try {
      await confirm.onConfirm();
    } catch (err) {
      setError((err as any)?.message || 'Action failed');
    } finally {
      setConfirmLoading(false);
      setConfirm(null);
    }
  };

  const permanentDeleteNote = (id: string) => {
    setConfirm({
      title: 'Delete note permanently',
      message: 'This note will be permanently deleted. This action cannot be undone.',
      onConfirm: async () => {
        await api.notes.delete(id, true);
        setNotes(prev => prev.filter(n => n.id !== id));
        toast.success('Note permanently deleted');
      },
    });
  };

  const permanentDeleteTodo = (id: string) => {
    setConfirm({
      title: 'Delete todo permanently',
      message: 'This todo will be permanently deleted. This action cannot be undone.',
      onConfirm: async () => {
        await api.todos.delete(id, true);
        setTodos(prev => prev.filter(t => t.id !== id));
        toast.success('Todo permanently deleted');
      },
    });
  };

  const emptyTrash = () => {
    setConfirm({
      title: 'Empty trash',
      message: 'All items in trash will be permanently deleted. This action cannot be undone.',
      onConfirm: async () => {
        await Promise.all([
          ...notes.map(n => api.notes.delete(n.id, true)),
          ...todos.map(t => api.todos.delete(t.id, true)),
        ]);
        setNotes([]);
        setTodos([]);
        toast.success('Trash emptied');
      },
    });
  };

  const totalItems = notes.length + todos.length;

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header
        title="Trash"
        actions={
          totalItems > 0 && (
            <button
              onClick={emptyTrash}
              className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
            >
              <i className="fas fa-trash-can mr-2"></i>
              Empty Trash
            </button>
          )
        }
      />

      <div className="flex-1 overflow-auto p-4 lg:p-8">
        {totalItems === 0 ? (
          <EmptyState icon="fa-trash" message="Trash is empty" />
        ) : (
          <>
            {notes.length > 0 && (
              <div className="mb-8">
                <h3 className="text-xl font-bold text-text mb-4">Deleted Notes ({notes.length})</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
                  {notes.map(note => (
                    <div key={note.id} className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border">
                      <div className="flex items-start justify-between mb-3">
                        <h4 className="font-bold text-text text-base lg:text-lg flex-1 pr-2">{note.title}</h4>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => restoreNote(note.id)}
                            className="text-primary hover:text-primary/80 p-1"
                            title="Restore"
                            aria-label="Restore note"
                          >
                            <i className="fas fa-rotate-left"></i>
                          </button>
                          <button
                            onClick={() => permanentDeleteNote(note.id)}
                            className="text-red-500 hover:text-red-600 p-1"
                            title="Delete permanently"
                            aria-label="Delete permanently"
                          >
                            <i className="fas fa-trash"></i>
                          </button>
                        </div>
                      </div>
                      <p className="text-text-secondary text-sm mb-3 line-clamp-3">{note.content || 'No content'}</p>
                      {note.deletedAt && (
                        <p className="text-xs text-text-secondary">
                          Deleted {new Date(note.deletedAt).toLocaleDateString()}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {todos.length > 0 && (
              <div>
                <h3 className="text-xl font-bold text-text mb-4">Deleted Todos ({todos.length})</h3>
                <div className="max-w-4xl space-y-4">
                  {todos.map(todo => (
                    <div key={todo.id} className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border">
                      <div className="flex items-start justify-between mb-2">
                        <h4 className="font-bold text-text text-base lg:text-lg flex-1 pr-2">{todo.title}</h4>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => restoreTodo(todo.id)}
                            className="text-primary hover:text-primary/80 p-1"
                            title="Restore"
                            aria-label="Restore todo"
                          >
                            <i className="fas fa-rotate-left"></i>
                          </button>
                          <button
                            onClick={() => permanentDeleteTodo(todo.id)}
                            className="text-red-500 hover:text-red-600 p-1"
                            title="Delete permanently"
                            aria-label="Delete permanently"
                          >
                            <i className="fas fa-trash"></i>
                          </button>
                        </div>
                      </div>
                      {todo.description && (
                        <p className="text-text-secondary mb-3">{todo.description}</p>
                      )}
                      {todo.deletedAt && (
                        <p className="text-xs text-text-secondary">
                          Deleted {new Date(todo.deletedAt).toLocaleDateString()}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <ConfirmDialog
        isOpen={confirm !== null}
        onClose={() => setConfirm(null)}
        onConfirm={runConfirm}
        title={confirm?.title ?? ''}
        message={confirm?.message ?? ''}
        confirmLabel="Delete permanently"
        variant="danger"
        loading={confirmLoading}
      />
    </PageShell>
  );
};
