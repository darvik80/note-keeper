/**
 * @module Search
 * @category Pages
 * @description Full-text search page with saved queries.
 */
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {EmptyState} from '../components/EmptyState';
import {Modal} from '../components/Modal';
import {MarkdownPreview} from '../components/MarkdownPreview';
import {useToast} from '../contexts/ToastContext';
import {api} from '../utils/api';
import {Note, SavedQuery, SavedQueryInput, Todo} from '../types';

/** Search page with full-text query input, result display, and saved query management. */
export const Search: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
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
      toast.success('Query saved');
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
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header title="Search" subtitle="Full-text across notes and todos" />

      <div className="flex-1 overflow-auto px-5 sm:px-8 lg:px-10 pb-8">
        <div className="max-w-3xl">
          <form onSubmit={handleSearch} className="mb-8">
            <div className="flex gap-2 mb-4">
              <div className="flex-1 relative">
                <i className="fas fa-search absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary"></i>
                <input
                  type="text"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search notes and todos…"
                  className="input-field pl-11 py-3"
                  autoFocus
                />
              </div>
              <button type="submit" className="nk-btn nk-btn-primary px-5">
                Search
              </button>
              {query && (
                <button
                  type="button"
                  onClick={() => setShowSaveDialog(true)}
                  className="nk-btn nk-btn-ghost"
                  title="Save Query"
                >
                  <i className="fas fa-bookmark"></i>
                </button>
              )}
            </div>

            <div className="flex gap-1 p-1 rounded-full bg-hover w-fit">
              {(['all', 'notes', 'todos'] as const).map(type => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setSearchType(type)}
                  className={`px-4 py-1.5 rounded-full text-[13px] font-medium transition-colors duration-[180ms] ${
                    searchType === type
                      ? 'bg-surface text-primary'
                      : 'text-text-secondary hover:text-text'
                  }`}
                >
                  {type.charAt(0).toUpperCase() + type.slice(1)}
                </button>
              ))}
            </div>
          </form>

          {savedQueries.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-bold text-text mb-3">Saved Queries</h3>
              <div className="flex flex-wrap gap-2">
                {savedQueries.map(sq => (
                  <div
                    key={sq.id}
                    className="flex items-center gap-2 bg-surface border border-border rounded-lg px-3 py-2"
                  >
                    <button
                      onClick={() => loadSavedQuery(sq)}
                      className="text-sm text-primary hover:underline"
                    >
                      {sq.name}
                    </button>
                    <button
                      onClick={() => deleteSavedQuery(sq.id)}
                      className="text-text-secondary hover:text-red-500"
                    >
                      <i className="fas fa-times text-xs"></i>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {query && (
            <div className="mb-4 text-sm text-text-secondary">
              Found {results.notes.length} notes and {results.todos.length} todos
            </div>
          )}

          {results.notes.length > 0 && (
            <div className="mb-8">
              <h3 className="text-lg font-bold text-text mb-4">Notes</h3>
              <div className="space-y-3">
                {results.notes.map(note => (
                  <div
                    key={note.id}
                    className="nk-card nk-card-hover p-5 cursor-pointer"
                    onClick={() => navigate(`/notes/${note.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-text">{note.title}</h4>
                      {note.isFavorite && <i className="fas fa-star text-yellow-500"></i>}
                    </div>
                    <MarkdownPreview content={note.content} maxLines={3} className="text-sm text-text-secondary mb-2" />
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-xs text-text-secondary">
                        <i className="fas fa-folder mr-1"></i>
                        {note.folder}
                      </span>
                      {note.tags.slice(0, 3).map(tag => (
                        <span key={tag} className="nk-tag">
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
              <h3 className="text-lg font-bold text-text mb-4">Todos</h3>
              <div className="space-y-3">
                {results.todos.map(todo => (
                  <div
                    key={todo.id}
                    className="nk-card nk-card-hover p-5 cursor-pointer"
                    onClick={() => navigate(`/todos/${todo.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-text">{todo.title}</h4>
                      {todo.isFavorite && <i className="fas fa-star text-yellow-500"></i>}
                    </div>
                    <MarkdownPreview content={todo.description} maxLines={3} className="text-sm text-text-secondary mb-2" />
                    <div className="flex items-center gap-3 text-xs text-text-secondary">
                      {todo.dueDate && (
                        <span>
                          <i className="fas fa-calendar mr-1"></i>
                          {new Date(todo.dueDate).toLocaleDateString()}
                        </span>
                      )}
                      <span className={`px-2 py-1 rounded ${
                        todo.completed ? 'bg-green-500/15 text-green-500' : 'bg-yellow-500/15 text-yellow-500'
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
            <EmptyState icon="fa-search" message={`No results for “${query}”`} />
          )}
        </div>
      </div>

      {showSaveDialog && (
        <Modal
          isOpen={showSaveDialog}
          onClose={() => setShowSaveDialog(false)}
          title="Save Query"
          footer={
            <>
              <button
                onClick={() => setShowSaveDialog(false)}
                className="flex-1 nk-btn nk-btn-ghost border border-border"
              >
                Cancel
              </button>
              <button
                onClick={saveQuery}
                className="flex-1 nk-btn nk-btn-primary"
              >
                Save
              </button>
            </>
          }
        >
          <input
            type="text"
            value={queryName}
            onChange={(e) => setQueryName(e.target.value)}
            placeholder="Query name"
            className="input-field"
            autoFocus
          />
        </Modal>
      )}
    </PageShell>
  );
};
