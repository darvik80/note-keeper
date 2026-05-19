/**
 * @module Trash
 * @category Pages
 * @description Trash page — view, restore, and permanently delete soft-deleted items.
 */
import React, { useState, useEffect } from 'react';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Note, Todo } from '../types';

/** Trash page listing soft-deleted notes and todos with restore and hard-delete actions. */
export const Trash: React.FC = () => {
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDeleted();
  }, []);

  const loadDeleted = async () => {
    try {
      const [n, t] = await Promise.all([
        api.notes.getAll({ isDeleted: true }),
        api.todos.getAll({ isDeleted: true })
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
    } catch (err) {
      setError((err as any)?.message || 'Failed to restore note');
    }
  };

  const restoreTodo = async (id: string) => {
    try {
      await api.todos.restore(id);
      setTodos(prev => prev.filter(t => t.id !== id));
    } catch (err) {
      setError((err as any)?.message || 'Failed to restore todo');
    }
  };

  const permanentDeleteNote = async (id: string) => {
    if (confirm('Permanently delete this note? This cannot be undone.')) {
      try {
        await api.notes.delete(id, true);
        setNotes(prev => prev.filter(n => n.id !== id));
      } catch (err) {
        setError((err as any)?.message || 'Failed to permanently delete note');
      }
    }
  };

  const permanentDeleteTodo = async (id: string) => {
    if (confirm('Permanently delete this todo? This cannot be undone.')) {
      try {
        await api.todos.delete(id, true);
        setTodos(prev => prev.filter(t => t.id !== id));
      } catch (err) {
        setError((err as any)?.message || 'Failed to permanently delete todo');
      }
    }
  };

  const emptyTrash = async () => {
    if (confirm('Empty trash? All items will be permanently deleted. This cannot be undone.')) {
      try {
        await Promise.all([
          ...notes.map(n => api.notes.delete(n.id, true)),
          ...todos.map(t => api.todos.delete(t.id, true))
        ]);
        setNotes([]);
        setTodos([]);
      } catch (err) {
        setError((err as any)?.message || 'Failed to empty trash');
        loadDeleted();
      }
    }
  };

  const totalItems = notes.length + todos.length;

  return (
    <div className="flex-1 flex flex-col bg-gray-50">
      {error && (
        <div className="flex items-center gap-3 px-4 py-3 bg-red-50 border-b border-red-200 text-red-700 text-sm">
          <i className="fas fa-circle-exclamation shrink-0"></i>
          <span className="flex-1">{error}</span>
          <button onClick={() => setError(null)} className="shrink-0 hover:text-red-900">
            <i className="fas fa-times"></i>
          </button>
        </div>
      )}
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
          <div className="text-center py-16">
            <i className="fas fa-trash text-6xl text-gray-300 mb-4"></i>
            <p className="text-gray-500 text-lg">Trash is empty</p>
          </div>
        ) : (
          <>
            {notes.length > 0 && (
              <div className="mb-8">
                <h3 className="text-xl font-bold text-dark mb-4">Deleted Notes ({notes.length})</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
                  {notes.map(note => (
                    <div
                      key={note.id}
                      className="bg-white rounded-xl p-5 lg:p-6 shadow-sm border border-gray-100"
                    >
                      <div className="flex items-start justify-between mb-3">
                        <h4 className="font-bold text-dark text-base lg:text-lg flex-1 pr-2">{note.title}</h4>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => restoreNote(note.id)}
                            className="text-primary hover:text-primary/80 p-1"
                            title="Restore"
                          >
                            <i className="fas fa-rotate-left"></i>
                          </button>
                          <button
                            onClick={() => permanentDeleteNote(note.id)}
                            className="text-red-500 hover:text-red-700 p-1"
                            title="Delete permanently"
                          >
                            <i className="fas fa-trash"></i>
                          </button>
                        </div>
                      </div>
                      <p className="text-gray-600 text-sm mb-3 line-clamp-3">{note.content || 'No content'}</p>
                      {note.deletedAt && (
                        <p className="text-xs text-gray-400">
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
                <h3 className="text-xl font-bold text-dark mb-4">Deleted Todos ({todos.length})</h3>
                <div className="max-w-4xl space-y-4">
                  {todos.map(todo => (
                    <div
                      key={todo.id}
                      className="bg-white rounded-xl p-5 lg:p-6 shadow-sm border border-gray-100"
                    >
                      <div className="flex items-start justify-between mb-2">
                        <h4 className="font-bold text-dark text-base lg:text-lg flex-1 pr-2">{todo.title}</h4>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => restoreTodo(todo.id)}
                            className="text-primary hover:text-primary/80 p-1"
                            title="Restore"
                          >
                            <i className="fas fa-rotate-left"></i>
                          </button>
                          <button
                            onClick={() => permanentDeleteTodo(todo.id)}
                            className="text-red-500 hover:text-red-700 p-1"
                            title="Delete permanently"
                          >
                            <i className="fas fa-trash"></i>
                          </button>
                        </div>
                      </div>
                      {todo.description && (
                        <p className="text-gray-600 mb-3">{todo.description}</p>
                      )}
                      {todo.deletedAt && (
                        <p className="text-xs text-gray-400">
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
    </div>
  );
};
