import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Todo, TodoInput } from '../types';

export const Todos: React.FC = () => {
  const navigate = useNavigate();
  const [todos, setTodos] = useState<Todo[]>([]);
  const [showCompleted, setShowCompleted] = useState(false);

  useEffect(() => {
    loadTodos();
  }, []);

  const loadTodos = async () => {
    try {
      const t = await api.todos.getAll({ isArchived: false, isDeleted: false });
      setTodos(t);
    } catch (err) {
      console.error('Failed to load todos', err);
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
      console.error('Failed to create todo', err);
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
        dueDate: todo.dueDate ? String(todo.dueDate) : undefined,
        reminder: todo.reminder ? String(todo.reminder) : undefined,
        location: todo.location,
        schedule: todo.schedule,
      });
    } catch (err) {
      setTodos(prev => prev.map(t => t.id === id ? { ...t, completed: !newVal } : t));
      console.error('Failed to toggle complete', err);
    }
  };

  const deleteTodo = async (id: string) => {
    try {
      await api.todos.delete(id);
      setTodos(prev => prev.filter(t => t.id !== id));
    } catch (err) {
      console.error('Failed to delete todo', err);
    }
  };

  const archiveTodo = async (id: string) => {
    try {
      await api.todos.archive(id);
      setTodos(prev => prev.filter(t => t.id !== id));
    } catch (err) {
      console.error('Failed to archive todo', err);
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
        dueDate: todo.dueDate ? String(todo.dueDate) : undefined,
        reminder: todo.reminder ? String(todo.reminder) : undefined,
        location: todo.location,
        schedule: todo.schedule,
      });
    } catch (err) {
      setTodos(prev => prev.map(t => t.id === id ? { ...t, isFavorite: !newVal } : t));
      console.error('Failed to toggle favorite', err);
    }
  };

  const filteredTodos = showCompleted ? todos : todos.filter(t => !t.completed);

  return (
    <div className="flex-1 flex flex-col bg-gray-50">
      <Header
        title="Todos"
        actions={
          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={showCompleted}
                onChange={(e) => setShowCompleted(e.target.checked)}
                className="w-4 h-4"
              />
              <span className="text-sm font-medium">Show Completed</span>
            </label>
            <button
              onClick={createTodo}
              className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
            >
              <i className="fas fa-plus mr-2"></i>
              New Todo
            </button>
          </div>
        }
      />

      <div className="flex-1 overflow-auto p-8">
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
                        className="text-gray-500 hover:text-gray-700"
                        title="Archive"
                      >
                        <i className="fas fa-box-archive"></i>
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          deleteTodo(todo.id);
                        }}
                        className="text-red-500 hover:text-red-700"
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
