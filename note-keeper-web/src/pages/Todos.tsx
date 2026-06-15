/**
 * @module Todos
 * @category Pages
 * @description Todos list page with filtering and todo management.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Todo, TodoInput } from '../types';

/** Full todos list page with filtering by status, priority, and tags. */
export const Todos: React.FC = () => {
  const navigate = useNavigate();
  const [todos, setTodos] = useState<Todo[]>([]);
  const [sharedTodos, setSharedTodos] = useState<Todo[]>([]);
  const [showCompleted, setShowCompleted] = useState(false);
  const [showSharedOnly, setShowSharedOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadTodos();
    loadSharedTodos();
  }, []);

  const loadTodos = async () => {
    try {
      const t = await api.todos.getAll({ isArchived: false, isDeleted: false });
      setTodos(t);
    } catch (err) {
      setError((err as any)?.message || 'Failed to load todos');
    }
  };

  const loadSharedTodos = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/todos/shared-with-me', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (response.ok) {
        const data = await response.json();
        setSharedTodos(data);
      }
    } catch (err) {
      setError((err as any)?.message || 'Failed to load shared todos');
    }
  };

  const createTodo = async () => {
    const input: TodoInput = {
      title: 'New Todo',
      description: '',
      tags: [],
      priority: 'medium',
      isFavorite: false,
      completed: false,
    };
    try {
      const newTodo = await api.todos.create(input);
      navigate(`/todos/${newTodo.id}`);
    } catch (err) {
      setError((err as any)?.message || 'Failed to create todo');
    }
  };

  const toggleComplete = async (id: string) => {
    const todo = todos.find(t => t.id === id);
    if (!todo) return;
    const newVal = !todo.completed;
    setTodos(prev => prev.map(t => t.id === id ? { ...t, completed: newVal } : t));
    try {
      await api.todos.update(id, {
        title: todo.title,
        description: todo.description,
        tags: todo.tags,
        priority: todo.priority,
        isFavorite: todo.isFavorite,
        completed: newVal,
        // Convert local date to ISO UTC format for backend
        dueDate: todo.dueDate ? (typeof todo.dueDate === 'string' ? todo.dueDate : todo.dueDate.toISOString()) : undefined,
        reminder: todo.reminder ? (typeof todo.reminder === 'string' ? todo.reminder : todo.reminder.toISOString()) : undefined,
        location: todo.location,
        schedule: todo.schedule ? {
          repeat: todo.schedule.repeat,
          endDate: todo.schedule.endDate ? (typeof todo.schedule.endDate === 'string' ? todo.schedule.endDate : todo.schedule.endDate.toISOString()) : undefined
        } : undefined,
      });
    } catch (err) {
      setTodos(prev => prev.map(t => t.id === id ? { ...t, completed: !newVal } : t));
      setError((err as any)?.message || 'Failed to toggle complete');
    }
  };

  const deleteTodo = async (id: string) => {
    try {
      await api.todos.delete(id);
      setTodos(prev => prev.filter(t => t.id !== id));
    } catch (err) {
      setError((err as any)?.message || 'Failed to delete todo');
    }
  };

  const archiveTodo = async (id: string) => {
    try {
      await api.todos.archive(id);
      setTodos(prev => prev.filter(t => t.id !== id));
    } catch (err) {
      setError((err as any)?.message || 'Failed to archive todo');
    }
  };

  const toggleFavorite = async (id: string) => {
    const todo = todos.find(t => t.id === id);
    if (!todo) return;
    const newVal = !todo.isFavorite;
    setTodos(prev => prev.map(t => t.id === id ? { ...t, isFavorite: newVal } : t));
    try {
      await api.todos.update(id, {
        title: todo.title,
        description: todo.description,
        tags: todo.tags,
        priority: todo.priority,
        isFavorite: newVal,
        completed: todo.completed,
        // Convert local date to ISO UTC format for backend
        dueDate: todo.dueDate ? (typeof todo.dueDate === 'string' ? todo.dueDate : todo.dueDate.toISOString()) : undefined,
        reminder: todo.reminder ? (typeof todo.reminder === 'string' ? todo.reminder : todo.reminder.toISOString()) : undefined,
        location: todo.location,
        schedule: todo.schedule ? {
          repeat: todo.schedule.repeat,
          endDate: todo.schedule.endDate ? (typeof todo.schedule.endDate === 'string' ? todo.schedule.endDate : todo.schedule.endDate.toISOString()) : undefined
        } : undefined,
      });
    } catch (err) {
      setTodos(prev => prev.map(t => t.id === id ? { ...t, isFavorite: !newVal } : t));
      setError((err as any)?.message || 'Failed to toggle favorite');
    }
  };

  const displayTodos = showSharedOnly ? sharedTodos : todos;

  const filteredTodos = showSharedOnly 
    ? (showCompleted ? displayTodos : displayTodos.filter(t => !t.completed))
    : (showCompleted ? todos : todos.filter(t => !t.completed));

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
        title="Todos"
        actions={
          <div className="flex items-center gap-2">
            <label className="flex items-center gap-2 cursor-pointer shrink-0">
              <input
                type="checkbox"
                checked={showCompleted}
                onChange={(e) => setShowCompleted(e.target.checked)}
                className="w-4 h-4"
              />
              <span className="text-sm font-medium hidden sm:inline">Show Completed</span>
              <span className="text-sm font-medium sm:hidden">Done</span>
            </label>
            <button
              onClick={() => setShowSharedOnly(!showSharedOnly)}
              className={`p-2 sm:px-4 sm:py-2 rounded-lg transition-colors ${
                showSharedOnly ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
              title="Shared with Me"
            >
              <i className="fas fa-share-alt sm:mr-2"></i>
              <span className="hidden sm:inline">Shared</span>
            </button>
            <button
              onClick={createTodo}
              className="p-2 sm:px-4 sm:py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
              title="New Todo"
            >
              <i className="fas fa-plus sm:mr-2"></i>
              <span className="hidden sm:inline">New Todo</span>
            </button>
          </div>
        }
      />

      <div className="flex-1 overflow-auto p-4 sm:p-8">
        <div className="max-w-4xl mx-auto space-y-4">
          {filteredTodos.map(todo => (
            <div
              key={todo.id}
              className={`bg-white rounded-xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-all ${
                todo.completed ? 'opacity-60' : ''
              }`}
            >
              <div className="flex items-start gap-4">
                <button
                  onClick={() => toggleComplete(todo.id)}
                  className={`mt-1 w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                    todo.completed
                      ? 'bg-primary border-primary text-white'
                      : 'border-gray-300 hover:border-primary'
                  }`}
                >
                  {todo.completed && <i className="fas fa-check text-xs"></i>}
                </button>

                <div className="flex-1 cursor-pointer" onClick={() => navigate(`/todos/${todo.id}`)}>
                  <div className="flex items-start justify-between mb-2">
                    <h3 className={`font-bold text-lg ${todo.completed ? 'line-through text-gray-500' : 'text-dark'}`}>
                      {todo.title}
                    </h3>
                    <div className="flex items-center gap-2">
                      <span className={`text-xs px-2 py-1 rounded ${
                        todo.priority === 'high' ? 'bg-red-100 text-red-700' :
                        todo.priority === 'medium' ? 'bg-yellow-100 text-yellow-700' :
                        'bg-green-100 text-green-700'
                      }`}>
                        {todo.priority}
                      </span>
                      {todo.sharedWith && todo.sharedWith !== '[]' && (
                        <i className="fas fa-share-alt text-green-500" title="Shared"></i>
                      )}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleFavorite(todo.id);
                        }}
                        className="hover:scale-110 transition-transform"
                      >
                        <i className={`fas fa-star ${todo.isFavorite ? 'text-yellow-500' : 'text-gray-300'}`}></i>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          archiveTodo(todo.id);
                        }}
                        className="p-1 text-gray-500 hover:text-gray-700"
                        title="Archive"
                      >
                        <i className="fas fa-box-archive"></i>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteTodo(todo.id);
                        }}
                        className="p-1 text-red-500 hover:text-red-700"
                        title="Delete"
                      >
                        <i className="fas fa-trash"></i>
                      </button>
                    </div>
                  </div>

                  {todo.description && (
                    <p className="text-gray-600 mb-3">{todo.description}</p>
                  )}

                  <div className="flex items-center gap-4 text-sm text-gray-500">
                    {todo.dueDate && (
                      <span>
                        <i className="fas fa-calendar mr-1"></i>
                        {new Date(todo.dueDate).toLocaleDateString()}
                      </span>
                    )}
                    {todo.location && (
                      <span>
                        <i className="fas fa-location-dot mr-1"></i>
                        {todo.location.address}
                      </span>
                    )}
                    {todo.schedule && todo.schedule.repeat !== 'none' && (
                      <span>
                        <i className="fas fa-repeat mr-1"></i>
                        {todo.schedule.repeat}
                      </span>
                    )}
                  </div>

                  {todo.tags.length > 0 && (
                    <div className="flex flex-wrap gap-2 mt-3">
                      {todo.tags.map(tag => (
                        <span key={tag} className="text-xs bg-gray-100 px-2 py-1 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}

          {filteredTodos.length === 0 && (
            <div className="text-center py-16">
              <i className="fas fa-list-check text-6xl text-gray-300 mb-4"></i>
              <p className="text-gray-500 text-lg">No todos found</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
