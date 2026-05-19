/**
 * @module Dashboard
 * @category Pages
 * @description Dashboard page — overview with stats, recent notes, and recent todos.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Note, Todo } from '../types';

/** Dashboard home page showing key metrics and recent activity. */
export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [archivedCount, setArchivedCount] = useState(0);
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
    { label: 'Archived', value: archivedCount, icon: 'fa-box-archive', color: 'bg-purple-500' }
  ];

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
      <Header title="Dashboard" />
      
      <div className="flex-1 overflow-auto p-4 lg:p-8">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-6 mb-6 lg:mb-8">
          {stats.map((stat, index) => (
            <div key={index} className="bg-white rounded-xl p-4 lg:p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between mb-3 lg:mb-4">
                <div className={`${stat.color} w-10 h-10 lg:w-12 lg:h-12 rounded-lg flex items-center justify-center text-white`}>
                  <i className={`fas ${stat.icon} text-lg lg:text-xl`}></i>
                </div>
                <span className="text-2xl lg:text-3xl font-bold text-dark">{stat.value}</span>
              </div>
              <p className="text-gray-600 font-medium text-sm lg:text-base">{stat.label}</p>
            </div>
          ))}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 lg:gap-6">
          <div className="bg-white rounded-xl p-5 lg:p-6 shadow-sm border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base lg:text-lg font-bold text-dark">Recent Notes</h3>
              <button
                onClick={() => navigate('/notes')}
                className="text-primary hover:text-primary/80 text-sm font-medium"
              >
                View All
              </button>
            </div>
            <div className="space-y-3">
              {recentNotes.length === 0 ? (
                <p className="text-gray-400 text-center py-8">No notes yet</p>
              ) : (
                recentNotes.map(note => (
                  <div
                    key={note.id}
                    className="p-4 border border-border rounded-lg hover:border-primary transition-colors cursor-pointer bg-surface"
                    onClick={() => navigate(`/notes/${note.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-text text-sm lg:text-base pr-2">{note.title}</h4>
                      {note.isFavorite && <i className="fas fa-star text-yellow-500 text-sm lg:text-base flex-shrink-0"></i>}
                    </div>
                    <div className="text-sm text-text-secondary line-clamp-3 markdown-preview leading-relaxed">
                      {note.content.split('\n').slice(0, 3).map((line, i) => (
                        <p key={i}>{line || '\u00A0'}</p>
                      ))}
                    </div>
                    <div className="flex items-center gap-2 mt-2 flex-wrap">
                      {note.tags.slice(0, 3).map(tag => (
                        <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="bg-white rounded-xl p-5 lg:p-6 shadow-sm border border-gray-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base lg:text-lg font-bold text-dark">Pending Todos</h3>
              <button
                onClick={() => navigate('/todos')}
                className="text-primary hover:text-primary/80 text-sm font-medium"
              >
                View All
              </button>
            </div>
            <div className="space-y-3">
              {pendingTodos.length === 0 ? (
                <p className="text-gray-400 text-center py-8">No pending todos</p>
              ) : (
                pendingTodos.map(todo => (
                  <div
                    key={todo.id}
                    className="p-4 border border-gray-200 rounded-lg hover:border-primary transition-colors cursor-pointer"
                    onClick={() => navigate(`/todos/${todo.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-dark text-sm lg:text-base pr-2">{todo.title}</h4>
                      {todo.isFavorite && <i className="fas fa-star text-yellow-500 text-sm lg:text-base flex-shrink-0"></i>}
                    </div>
                    <p className="text-sm text-gray-600 leading-relaxed">{todo.description}</p>
                    {todo.dueDate && (
                      <p className="text-xs text-gray-500 mt-2">
                        <i className="fas fa-calendar mr-1"></i>
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
    </div>
  );
};
