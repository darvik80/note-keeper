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
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [dropPath, setDropPath] = useState<string | null>(null);

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

  const moveNote = async (id: string, folderPath: string) => {
    const note = notes.find(n => n.id === id);
    setDraggingId(null);
    setDropPath(null);
    if (!note) return;
    const { folder, subfolder } = parsePath(folderPath);
    const same =
      (note.folder || 'default') === folder &&
      (note.subfolder || '') === (subfolder || '');
    if (same) return;
    const prevFolder = note.folder;
    const prevSub = note.subfolder;
    setNotes(prev => prev.map(n => n.id === id ? { ...n, folder, subfolder } : n));
    try {
      await api.notes.update(id, {
        title: note.title,
        content: note.content,
        tags: note.tags,
        folder,
        subfolder,
        priority: note.priority,
        isFavorite: note.isFavorite,
        isEncrypted: note.isEncrypted,
        reminder: note.reminder ? new Date(note.reminder as unknown as string).toISOString() : undefined,
      });
      toast.success(folderPath === 'root' ? 'Moved to All Notes' : `Moved to ${folderPath}`);
    } catch (err) {
      setNotes(prev => prev.map(n => n.id === id ? { ...n, folder: prevFolder, subfolder: prevSub } : n));
      setError((err as any)?.message || 'Failed to move note');
    }
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
        subtitle={showSharedOnly ? 'Shared with you' : `${filteredNotes.length} in view`}
        actions={
          <>
            <button
              onClick={() => setShowFolderPanel(!showFolderPanel)}
              className={`h-9 w-9 inline-flex items-center justify-center rounded-full border border-border shrink-0 transition-colors duration-[180ms] ${
                showFolderPanel ? 'text-primary bg-primary/15' : 'text-text-secondary hover:bg-hover hover:text-text'
              }`}
              title="Toggle folders"
              aria-label="Toggle folders panel"
            >
              <i className="fas fa-folder-tree text-sm"></i>
            </button>
            <button
              onClick={() => setShowImport(!showImport)}
              className={`h-9 w-9 inline-flex items-center justify-center rounded-full border border-border shrink-0 transition-colors duration-[180ms] ${
                showImport ? 'text-primary bg-primary/15' : 'text-text-secondary hover:bg-hover hover:text-text'
              }`}
              title="Import"
              aria-label="Import"
            >
              <i className="fas fa-file-import text-sm"></i>
            </button>
            <button
              onClick={() => setShowSharedOnly(!showSharedOnly)}
              className={`nk-btn h-9 shrink-0 ${showSharedOnly ? 'nk-btn-primary' : 'nk-btn-ghost border border-border'}`}
              title="Shared with Me"
            >
              <i className="fas fa-share-alt sm:hidden" aria-hidden="true"></i>
              <span className="hidden sm:inline">Shared</span>
            </button>
            <button
              onClick={createNote}
              className="nk-btn nk-btn-primary h-9 shrink-0"
              title="New Note"
            >
              <i className="fas fa-plus"></i>
              <span className="hidden sm:inline">New</span>
            </button>
          </>
        }
      />

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
            <div className="fixed lg:static inset-y-0 left-0 z-30 lg:z-auto w-56 bg-background lg:bg-transparent overflow-y-auto p-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-[11px] font-medium uppercase tracking-wider text-text-secondary">
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
                dragging={!showSharedOnly && draggingId != null}
                dropPath={dropPath}
                onFolderDragOver={setDropPath}
                onFolderDragLeave={() => setDropPath(null)}
                onDropNote={(path, noteId) => moveNote(noteId, path)}
              />
              {!showSharedOnly && (
                <p className="px-3 mt-3 text-[12px] text-text-secondary leading-snug">
                  Drop a note on a folder to move it.
                </p>
              )}
            </div>
          </>
        )}

        <div className="flex-1 overflow-y-auto px-5 sm:px-8 lg:px-10 pb-8">
          <div className="relative mb-4">
            <input
              type="text"
              placeholder="Filter notes…"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full h-10 pl-10 pr-4 rounded-full border border-border bg-surface text-sm text-text"
            />
            <i className="fas fa-search absolute left-3.5 top-1/2 -translate-y-1/2 text-text-secondary text-xs pointer-events-none" aria-hidden="true"></i>
          </div>
          {loading ? (
            <CardGridSkeleton />
          ) : filteredNotes.length === 0 ? (
            <EmptyState
              icon="fa-note-sticky"
              message={searchTerm ? 'No matching notes' : 'No notes yet'}
              hint={searchTerm ? undefined : selectedFolder === 'root' ? 'Write the first one.' : 'Drag a note here from All Notes, or create one.'}
              action={!searchTerm ? (
                <button onClick={createNote} className="nk-btn nk-btn-primary">New note</button>
              ) : undefined}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
              {filteredNotes.map(note => (
                <NoteCard
                  key={note.id}
                  note={note}
                  canDrag={!showSharedOnly}
                  dragging={draggingId === note.id}
                  onDragBegin={setDraggingId}
                  onDragEnd={() => { setDraggingId(null); setDropPath(null); }}
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
