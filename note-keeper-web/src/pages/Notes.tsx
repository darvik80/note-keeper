/**
 * @module Notes
 * @category Pages
 * @description Notes list page with folder panel, search, filtering, and note management.
 */
import React, {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {FolderTree} from '../components/FolderTree';
import {PageShell} from '../components/PageShell';
import {NoteCard} from '../components/NoteCard';
import {EmptyState} from '../components/EmptyState';
import {CardGridSkeleton} from '../components/LoadingSkeleton';
import {api} from '../utils/api';
import {buildFolderTree, getFullPath, parsePath} from '../utils/folderUtils';
import {Note, NoteInput} from '../types';
import {useWebSocket} from '../hooks/useWebSocket';
import {useMediaQuery} from '../hooks/useMediaQuery';
import {useToast} from '../contexts/ToastContext';

export const Notes: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const isDesktop = useMediaQuery('(min-width: 1024px)');
  const [notes, setNotes] = useState<Note[]>([]);
  const [sharedNotes, setSharedNotes] = useState<Note[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFolder, setSelectedFolder] = useState<string>('root');
  const [showImport, setShowImport] = useState(false);
  const [showFolderPanel, setShowFolderPanel] = useState(isDesktop);
  const [showSharedOnly, setShowSharedOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isDesktop) setShowFolderPanel(true);
  }, [isDesktop]);

  const loadNotes = useCallback(async () => {
    try {
      const n = await api.notes.getAll({ isArchived: false, isDeleted: false });
      setNotes(n);
    } catch (err) {
      setError((err as any)?.message || 'Failed to load notes');
    }
  }, []);

  const loadSharedNotes = useCallback(async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/notes/shared-with-me', {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (response.ok) {
        setSharedNotes(await response.json());
      }
    } catch (err) {
      setError((err as any)?.message || 'Failed to load shared notes');
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    Promise.all([loadNotes(), loadSharedNotes()]).finally(() => setLoading(false));
  }, [loadNotes, loadSharedNotes]);

  useWebSocket((event) => {
    if (event.type.startsWith('NOTE_')) {
      loadNotes();
      loadSharedNotes();
    }
  });

  const createNote = async () => {
    const { folder, subfolder } = parsePath(selectedFolder);
    const input: NoteInput = {
      title: 'Untitled Note',
      content: '',
      tags: [],
      folder: folder === 'root' ? 'default' : folder,
      subfolder,
      priority: 'medium',
      isFavorite: false,
      isEncrypted: false,
    };
    try {
      const newNote = await api.notes.create(input);
      navigate(`/notes/${newNote.id}`);
    } catch (err) {
      setError((err as any)?.message || 'Failed to create note');
    }
  };

  const deleteNote = async (id: string) => {
    try {
      await api.notes.delete(id);
      setNotes(prev => prev.filter(n => n.id !== id));
      toast.success('Note moved to trash');
    } catch (err) {
      setError((err as any)?.message || 'Failed to delete note');
    }
  };

  const archiveNote = async (id: string) => {
    try {
      await api.notes.archive(id);
      setNotes(prev => prev.filter(n => n.id !== id));
      toast.success('Note archived');
    } catch (err) {
      setError((err as any)?.message || 'Failed to archive note');
    }
  };

  const toggleFavorite = async (id: string) => {
    const note = notes.find(n => n.id === id);
    if (!note) return;
    const newVal = !note.isFavorite;
    setNotes(prev => prev.map(n => n.id === id ? { ...n, isFavorite: newVal } : n));
    try {
      await api.notes.update(id, {
        title: note.title,
        content: note.content,
        tags: note.tags,
        folder: note.folder,
        subfolder: note.subfolder,
        priority: note.priority,
        isFavorite: newVal,
        isEncrypted: note.isEncrypted,
      });
    } catch (err) {
      setNotes(prev => prev.map(n => n.id === id ? { ...n, isFavorite: !newVal } : n));
      setError((err as any)?.message || 'Failed to toggle favorite');
    }
  };

  const handleCreateFolder = (parentPath: string, name: string) => {
    const newPath = parentPath === 'root' ? name : `${parentPath}/${name}`;
    setSelectedFolder(newPath);
  };

  const folderTree = buildFolderTree(notes);
  const displayNotes = showSharedOnly ? sharedNotes : notes;

  const filteredNotes = displayNotes.filter(note => {
    const matchesSearch = note.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         note.content.toLowerCase().includes(searchTerm.toLowerCase());
    if (showSharedOnly) return matchesSearch;
    if (selectedFolder === 'root') return matchesSearch;
    const notePath = getFullPath(note.folder, note.subfolder);
    const matchesFolder = notePath === selectedFolder || notePath.startsWith(selectedFolder + '/');
    return matchesSearch && matchesFolder;
  });

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const { folder, subfolder } = parsePath(selectedFolder);
    try {
      const newNote = await api.notes.importFile(
        file,
        folder === 'root' ? 'imported' : folder,
        subfolder
      );
      setNotes(prev => [newNote, ...prev]);
      setShowImport(false);
    } catch (err) {
      setError((err as any)?.message || 'Failed to import file');
    }
  };

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header
        title="Notes"
        actions={
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowFolderPanel(!showFolderPanel)}
              className="p-2 hover:bg-hover rounded-lg transition-colors"
              title="Toggle folders"
              aria-label="Toggle folders panel"
            >
              <i className={`fas fa-folder-tree ${showFolderPanel ? 'text-primary' : 'text-text-secondary'}`}></i>
            </button>
            <div className="relative hidden sm:block">
              <input
                type="text"
                placeholder="Search notes..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="input-field pl-10 w-40 sm:w-56 lg:w-64 py-2"
              />
              <i className="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary"></i>
            </div>
            <button
              onClick={() => setShowImport(!showImport)}
              className="p-2 sm:px-4 sm:py-2 bg-secondary text-white rounded-lg hover:bg-secondary/90 transition-colors"
              title="Import"
            >
              <i className="fas fa-file-import sm:mr-2"></i>
              <span className="hidden sm:inline">Import</span>
            </button>
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
              onClick={createNote}
              className="p-2 sm:px-4 sm:py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
              title="New Note"
            >
              <i className="fas fa-plus sm:mr-2"></i>
              <span className="hidden sm:inline">New Note</span>
            </button>
          </div>
        }
      />

      <div className="sm:hidden px-4 py-2 bg-surface border-b border-border">
        <div className="relative">
          <input
            type="text"
            placeholder="Search notes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input-field pl-10 py-2"
          />
          <i className="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary"></i>
        </div>
      </div>

      {showImport && (
        <div className="bg-secondary/10 border-b border-secondary/20 px-4 lg:px-8 py-3">
          <div className="flex items-center gap-4 flex-wrap">
            <label className="text-sm font-medium text-text">
              Import from: MD, PDF, DOC, DOCX, XLS, CSV
            </label>
            <input type="file" accept=".md,.pdf,.doc,.docx,.xls,.csv" onChange={handleImport} className="text-sm" />
            <button onClick={() => setShowImport(false)} className="ml-auto text-secondary hover:opacity-80 p-1" aria-label="Close import">
              <i className="fas fa-times"></i>
            </button>
          </div>
        </div>
      )}

      <div className="flex-1 flex overflow-hidden relative min-h-0">
        {showFolderPanel && (
          <>
            <div
              className="fixed inset-0 bg-black/40 z-20 lg:hidden"
              onClick={() => setShowFolderPanel(false)}
              aria-hidden="true"
            />
            <div className="fixed lg:static inset-y-0 left-0 z-30 lg:z-auto w-64 bg-surface border-r border-border overflow-y-auto p-4 shadow-lg lg:shadow-none">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-bold text-text flex items-center gap-2">
                  <i className="fas fa-folder"></i>
                  Folders
                </h3>
                <button
                  onClick={() => setShowFolderPanel(false)}
                  className="lg:hidden p-1 text-text-secondary hover:text-text"
                  aria-label="Close folders panel"
                >
                  <i className="fas fa-times"></i>
                </button>
              </div>
              <FolderTree
                folders={folderTree}
                selectedPath={selectedFolder}
                onSelectFolder={(path) => {
                  setSelectedFolder(path);
                  if (!isDesktop) setShowFolderPanel(false);
                }}
                onCreateFolder={handleCreateFolder}
              />
            </div>
          </>
        )}

        <div className="flex-1 overflow-y-auto p-4 lg:p-8">
          {loading ? (
            <CardGridSkeleton />
          ) : filteredNotes.length === 0 ? (
            <EmptyState icon="fa-note-sticky" message="No notes found" />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
              {filteredNotes.map(note => (
                <NoteCard
                  key={note.id}
                  note={note}
                  onClick={() => navigate(`/notes/${note.id}`)}
                  onToggleFavorite={(e) => { e.stopPropagation(); toggleFavorite(note.id); }}
                  onArchive={(e) => { e.stopPropagation(); archiveNote(note.id); }}
                  onDelete={(e) => { e.stopPropagation(); deleteNote(note.id); }}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </PageShell>
  );
};
