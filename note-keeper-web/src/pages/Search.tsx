/**
 * @module Search
 * @category Pages
 * @description Full-text search page with saved queries.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Note, Todo, SavedQuery, SavedQueryInput } from '../types';

/** Search page with full-text query input, result display, and saved query management. */
export const Search: React.FC = () => {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [searchType, setSearchType] = useState<'all' | 'notes' | 'todos'>('all');
  const [results, setResults] = useState<{ notes: Note[]; todos: Todo[] }>({ notes: [], todos: [] });
  const [savedQueries, setSavedQueries] = useState<SavedQuery[]>([]);
  const [showSaveDialog, setShowSaveDialog] = useState(false);
  const [queryName, setQueryName] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const q = await api.search.getSavedQueries();
        setSavedQueries(q);
      } catch (err) {
        setError((err as any)?.message || 'Failed to load saved queries');
      }
    };
    load();
  }, []);

  const performSearch = async (searchQuery: string, type: 'all' | 'notes' | 'todos' = searchType) => {
    if (!searchQuery.trim()) {
      setResults({ notes: [], todos: [] });
      return;
    }
    try {
      const typeParam = type === 'all' ? undefined : type;
      const result = await api.search.search(searchQuery, typeParam);
      setResults(result);
    } catch (err) {
      setError((err as any)?.message || 'Failed to search');
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    performSearch(query);
  };

  const saveQuery = async () => {
    if (!queryName.trim() || !query.trim()) return;
    const input: SavedQueryInput = {
      name: queryName,
      query: query,
      filters: { type: searchType },
    };
    try {
      const saved = await api.search.saveQuery(input);
      setSavedQueries(prev => [saved, ...prev]);
      setShowSaveDialog(false);
      setQueryName('');
    } catch (err) {
      setError((err as any)?.message || 'Failed to save query');
    }
  };

  const loadSavedQuery = (savedQuery: SavedQuery) => {
    setQuery(savedQuery.query);
    setSearchType(savedQuery.filters.type || 'all');
    performSearch(savedQuery.query, savedQuery.filters.type || 'all');
  };

  const deleteSavedQuery = async (id: string) => {
    try {
      await api.search.deleteQuery(id);
      setSavedQueries(prev => prev.filter(q => q.id !== id));
    } catch (err) {
      setError((err as any)?.message || 'Failed to delete query');
    }
  };

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
      <Header title="Search" />

      <div className="flex-1 overflow-auto p-8">
        <div className="max-w-4xl mx-auto">
          <form onSubmit={handleSearch} className="mb-6">
            <div className="flex gap-3 mb-4">
              <div className="flex-1 relative">
                <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-gray-400"></i>
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search notes and todos..."
                  className="w-full pl-12 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:border-primary"
                  autoFocus
                />
              </div>
              <button
                type="submit"
                className="px-6 py-3 bg-primary text-white rounded-lg hover:bg-primary/90"
              >
                Search
              </button>
              {query && (
                <button
                  type="button"
                  onClick={() => setShowSaveDialog(true)}
                  className="px-4 py-3 border border-gray-300 rounded-lg hover:bg-gray-50"
                  title="Save Query"
                >
                  <i className="fas fa-bookmark"></i>
                </button>
              )}
            </div>

            <div className="flex gap-2">
              {(['all', 'notes', 'todos'] as const).map(type => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setSearchType(type)}
                  className={`px-4 py-2 rounded-lg transition-colors ${
                    searchType === type
                      ? 'bg-primary text-white'
                      : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  {type.charAt(0).toUpperCase() + type.slice(1)}
                </button>
              ))}
            </div>
          </form>

          {savedQueries.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-bold text-gray-700 mb-3">Saved Queries</h3>
              <div className="flex flex-wrap gap-2">
                {savedQueries.map(sq => (
                  <div
                    key={sq.id}
                    className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg px-3 py-2"
                  >
                    <button
                      onClick={() => loadSavedQuery(sq)}
                      className="text-sm text-primary hover:underline"
                    >
                      {sq.name}
                    </button>
                    <button
                      onClick={() => deleteSavedQuery(sq.id)}
                      className="text-gray-400 hover:text-red-500"
                    >
                      <i className="fas fa-times text-xs"></i>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {query && (
            <div className="mb-4 text-sm text-gray-600">
              Found {results.notes.length} notes and {results.todos.length} todos
            </div>
          )}

          {results.notes.length > 0 && (
            <div className="mb-8">
              <h3 className="text-lg font-bold text-dark mb-4">Notes</h3>
              <div className="space-y-3">
                {results.notes.map(note => (
                  <div
                    key={note.id}
                    className="bg-white p-4 rounded-lg border border-gray-200 hover:border-primary transition-colors cursor-pointer"
                    onClick={() => navigate(`/notes/${note.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-dark">{note.title}</h4>
                      {note.isFavorite && <i className="fas fa-star text-yellow-500"></i>}
                    </div>
                    <p className="text-sm text-gray-600 line-clamp-2 mb-2">{note.content}</p>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-xs text-gray-500">
                        <i className="fas fa-folder mr-1"></i>
                        {note.folder}
                      </span>
                      {note.tags.slice(0, 3).map(tag => (
                        <span key={tag} className="text-xs bg-primary/10 text-primary px-2 py-1 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {results.todos.length > 0 && (
            <div>
              <h3 className="text-lg font-bold text-dark mb-4">Todos</h3>
              <div className="space-y-3">
                {results.todos.map(todo => (
                  <div
                    key={todo.id}
                    className="bg-white p-4 rounded-lg border border-gray-200 hover:border-primary transition-colors cursor-pointer"
                    onClick={() => navigate(`/todos/${todo.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-dark">{todo.title}</h4>
                      {todo.isFavorite && <i className="fas fa-star text-yellow-500"></i>}
                    </div>
                    <p className="text-sm text-gray-600 mb-2">{todo.description}</p>
                    <div className="flex items-center gap-3 text-xs text-gray-500">
                      {todo.dueDate && (
                        <span>
                          <i className="fas fa-calendar mr-1"></i>
                          {new Date(todo.dueDate).toLocaleDateString()}
                        </span>
                      )}
                      <span className={`px-2 py-1 rounded ${
                        todo.completed ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                      }`}>
                        {todo.completed ? 'Completed' : 'Pending'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {query && results.notes.length === 0 && results.todos.length === 0 && (
            <div className="text-center py-12 text-gray-400">
              <i className="fas fa-search text-4xl mb-4"></i>
              <p>No results found for "{query}"</p>
            </div>
          )}
        </div>
      </div>

      {showSaveDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-96">
            <h3 className="text-lg font-bold mb-4">Save Query</h3>
            <input
              type="text"
              value={queryName}
              onChange={(e) => setQueryName(e.target.value)}
              placeholder="Query name"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg mb-4"
              autoFocus
            />
            <div className="flex gap-3">
              <button
                onClick={saveQuery}
                className="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
              >
                Save
              </button>
              <button
                onClick={() => setShowSaveDialog(false)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
