/**
 * @module Todos
 * @category Pages
 * @description Todos list page with filtering and todo management.
 */
import React, {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {TodoCard} from '../components/TodoCard';
import {EmptyState} from '../components/EmptyState';
import {TodoListSkeleton} from '../components/LoadingSkeleton';
import {api} from '../utils/api';
import {Todo, TodoInput} from '../types';
import {useWebSocket} from '../hooks/useWebSocket';

export const Todos: React.FC = () => {
  const navigate = useNavigate();
  const [todos, setTodos] = useState<Todo[]>([]);
  const [sharedTodos, setSharedTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCompleted, setShowCompleted] = useState(false);
  const [showSharedOnly, setShowSharedOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadTodos = useCallback(async () => {
    try {
      const t = await api.todos.getAll({ isArchived: false, isDeleted: false });
      setTodos(t);
    } catch (err) {
      setError((err as any)?.message || 'Failed to load todos');
    }
  }, []);

  const loadSharedTodos = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/todos/shared-with-me', {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (response.ok) {
        setSharedTodos(await response.json());
      }
    } catch (err) {
      setError((err as any)?.message || 'Failed to load shared todos');
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    Promise.all([loadTodos(), loadSharedTodos()]).finally(() => setLoading(false));
  }, [loadTodos, loadSharedTodos]);

  useWebSocket((event) => {
    if (event.type.startsWith('TODO_')) {
      loadTodos();
      loadSharedTodos();
    }
  });

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

  const buildUpdatePayload = (todo: Todo, overrides: Partial<TodoInput>) => ({
    title: todo.title,
    description: todo.description,
    tags: todo.tags,
    priority: todo.priority,
    isFavorite: todo.isFavorite,
    completed: todo.completed,
    dueDate: todo.dueDate ? (typeof todo.dueDate === 'string' ? todo.dueDate : todo.dueDate.toISOString()) : undefined,
    reminder: todo.reminder ? (typeof todo.reminder === 'string' ? todo.reminder : todo.reminder.toISOString()) : undefined,
    location: todo.location,
    schedule: todo.schedule ? { repeat: todo.schedule.repeat, endDate: todo.schedule.endDate || undefined } : undefined,
    ...overrides,
  });

  const toggleComplete = async (id: string) => {
    const todo = todos.find(t => t.id === id);
    if (!todo) return;
    const newVal = !todo.completed;
    setTodos(prev => prev.map(t => t.id === id ? { ...t, completed: newVal } : t));
    try {
      await api.todos.update(id, buildUpdatePayload(todo, { completed: newVal }));
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
      await api.todos.update(id, buildUpdatePayload(todo, { isFavorite: newVal }));
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
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header
        title="Todos"
        actions={
          <div className="flex items-center gap-2">
            <label className="flex items-center gap-2 cursor-pointer shrink-0">
              <input
                type="checkbox"
                checked={showCompleted}
                onChange={(e) => setShowCompleted(e.target.checked)}
                className="w-4 h-4 accent-primary"
              />
              <span className="text-sm font-medium hidden sm:inline">Show Completed</span>
              <span className="text-sm font-medium sm:hidden">Done</span>
            </label>
            <button
              onClick={() => setShowSharedOnly(!showSharedOnly)}
              className={`p-2 sm:px-4 sm:py-2 rounded-lg transition-colors ${
                showSharedOnly ? 'bg-primary text-white' : 'btn-secondary'
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
        {loading ? (
          <TodoListSkeleton />
        ) : filteredTodos.length === 0 ? (
          <EmptyState icon="fa-list-check" message="No todos found" />
        ) : (
          <div className="max-w-4xl mx-auto space-y-4">
            {filteredTodos.map(todo => (
              <TodoCard
                key={todo.id}
                todo={todo}
                onNavigate={() => navigate(`/todos/${todo.id}`)}
                onToggleComplete={(e) => { e.stopPropagation(); toggleComplete(todo.id); }}
                onToggleFavorite={(e) => { e.stopPropagation(); toggleFavorite(todo.id); }}
                onArchive={(e) => { e.stopPropagation(); archiveTodo(todo.id); }}
                onDelete={(e) => { e.stopPropagation(); deleteTodo(todo.id); }}
              />
            ))}
          </div>
        )}
      </div>
    </PageShell>
  );
};
