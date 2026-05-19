/**
 * @module Favorites
 * @category Pages
 * @description Favorites page — all favorited notes and todos.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Note, Todo } from '../types';

/** Favorites page listing all items marked as favorite. */
export const Favorites: React.FC = () => {
  const navigate = useNavigate();
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
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
      }
    };
    load();
  }, []);

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
      <Header title="Favorites" />

      <div className="flex-1 overflow-auto p-8">
        <div className="mb-8">
          <h3 className="text-xl font-bold text-dark mb-4">Favorite Notes</h3>
          <div className="grid grid-cols-3 gap-6">
            {notes.map(note => (
              <div
                key={note.id}
                className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow cursor-pointer"
                onClick={() => navigate(`/notes/${note.id}`)}
              >
                <div className="flex items-start justify-between mb-3">
                  <h4 className="font-bold text-dark text-lg flex-1">{note.title}</h4>
                  <i className="fas fa-star text-yellow-500"></i>
                </div>
                <p className="text-gray-600 text-sm mb-4 line-clamp-3">{note.content || 'No content'}</p>
                <div className="flex flex-wrap gap-2">
                  {note.tags.slice(0, 3).map(tag => (
                    <span key={tag} className="text-xs bg-gray-100 px-2 py-1 rounded">
                      #{tag}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
          {notes.length === 0 && (
            <p className="text-gray-400 text-center py-8">No favorite notes</p>
          )}
        </div>

        <div>
          <h3 className="text-xl font-bold text-dark mb-4">Favorite Todos</h3>
          <div className="max-w-4xl space-y-4">
            {todos.map(todo => (
              <div
                key={todo.id}
                className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow cursor-pointer"
                onClick={() => navigate(`/todos/${todo.id}`)}
              >
                <div className="flex items-start justify-between mb-2">
                  <h4 className="font-bold text-dark text-lg flex-1">{todo.title}</h4>
                  <i className="fas fa-star text-yellow-500"></i>
                </div>
                {todo.description && (
                  <p className="text-gray-600 mb-3">{todo.description}</p>
                )}
                <div className="flex flex-wrap gap-2">
                  {todo.tags.map(tag => (
                    <span key={tag} className="text-xs bg-gray-100 px-2 py-1 rounded">
                      #{tag}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
          {todos.length === 0 && (
            <p className="text-gray-400 text-center py-8">No favorite todos</p>
          )}
        </div>
      </div>
    </div>
  );
};
