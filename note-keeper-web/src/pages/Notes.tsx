import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { FolderTree } from '../components/FolderTree';
import { api } from '../utils/api';
import { buildFolderTree, parsePath, getFullPath } from '../utils/folderUtils';
import { Note, NoteInput } from '../types';

export const Notes: React.FC = () => {
  const navigate = useNavigate();
  const [notes, setNotes] = useState<Note[]>([]);
  const [sharedNotes, setSharedNotes] = useState<Note[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFolder, setSelectedFolder] = useState<string>('root');
  const [showImport, setShowImport] = useState(false);
  const [showFolderPanel, setShowFolderPanel] = useState(true);
  const [showSharedOnly, setShowSharedOnly] = useState(false);

  useEffect(() => {
    loadNotes();
    loadSharedNotes();
  }, []);

  const loadNotes = async () => {
    try {
      const n = await api.notes.getAll({ isArchived: false, isDeleted: false });
      setNotes(n);
    } catch (err) {
      console.error('Failed to load notes', err);
    }
  };

  const loadSharedNotes = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/notes/shared-with-me', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (response.ok) {
        const data = await response.json();
        setSharedNotes(data);
      }
    } catch (err) {
      console.error('Failed to load shared notes', err);
    }
  };

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
      console.error('Failed to create note', err);
    }
  };

  const deleteNote = async (id: string) => {
    try {
      await api.notes.delete(id);
      setNotes(prev => prev.filter(n => n.id !== id));
    } catch (err) {
      console.error('Failed to delete note', err);
    }
  };

  const archiveNote = async (id: string) => {
    try {
      await api.notes.archive(id);
      setNotes(prev => prev.filter(n => n.id !== id));
    } catch (err) {
      console.error('Failed to archive note', err);
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
      console.error('Failed to toggle favorite', err);
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
      console.error('Failed to import file', err);
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-gray-50">
      <Header
        title="Notes"
        actions={
          <div className="flex items-center gap-3">
            <button
              onClick={() => setShowFolderPanel(!showFolderPanel)}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <i className={`fas fa-folder-tree ${showFolderPanel ? 'text-primary' : 'text-gray-600'}`}></i>
            </button>
            <div className="relative">
              <input
                type="text"
                placeholder="Search notes..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-primary w-64"
              />
              <i className="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"></i>
            </div>
            <button
              onClick={() => setShowImport(!showImport)}
              className="px-4 py-2 bg-secondary text-white rounded-lg hover:bg-secondary/90 transition-colors"
            >
              <i className="fas fa-file-import mr-2"></i>
              Import
            </button>
            <button
              onClick={() => setShowSharedOnly(!showSharedOnly)}
              className={`px-4 py-2 rounded-lg transition-colors ${
                showSharedOnly ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
              title="Show only notes shared with me"
            >
              <i className="fas fa-share-alt mr-2"></i>
              Shared with Me
            </button>
            <button
              onClick={createNote}
              className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
            >
              <i className="fas fa-plus mr-2"></i>
              New Note
            </button>
          </div>
        }
      />

      {showImport && (
        <div className="bg-blue-50 border-b border-blue-200 px-8 py-3">
          <div className="flex items-center gap-4">
            <label className="text-sm font-medium text-blue-900">
              Import from: MD, PDF, DOC, DOCX, XLS, CSV
            </label>
            <input
              type="file"
              accept=".md,.pdf,.doc,.docx,.xls,.csv"
              onChange={handleImport}
              className="text-sm"
            />
            <button
              onClick={() => setShowImport(false)}
              className="ml-auto text-blue-600 hover:text-blue-800"
            >
              <i className="fas fa-times"></i>
            </button>
          </div>
        </div>
      )}

      <div className="flex-1 flex overflow-hidden">
        {showFolderPanel && (
          <div className="w-64 bg-white border-r border-gray-200 overflow-y-auto p-4">
            <h3 className="text-sm font-bold text-gray-700 mb-3 flex items-center gap-2">
              <i className="fas fa-folder"></i>
              Folders
            </h3>
            <FolderTree
              folders={folderTree}
              selectedPath={selectedFolder}
              onSelectFolder={setSelectedFolder}
              onCreateFolder={handleCreateFolder}
            />
          </div>
        )}

        <div className="flex-1 overflow-auto p-4 lg:p-8">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
            {filteredNotes.map(note => (
              <div
                key={note.id}
                className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border hover:shadow-md transition-shadow cursor-pointer"
                onClick={() => navigate(`/notes/${note.id}`)}
              >
                <div className="flex items-start justify-between mb-3">
                  <h3 className="font-bold text-text text-base lg:text-lg flex-1 pr-2">{note.title}</h3>
                  <div className="flex items-center gap-2 lg:gap-2 flex-shrink-0">
                    {note.isEncrypted && <i className="fas fa-lock text-purple-500 text-sm lg:text-base"></i>}
                    {note.sharedWith && note.sharedWith !== '[]' && (
                      <i className="fas fa-share-alt text-green-500 text-sm lg:text-base" title="Shared"></i>
                    )}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleFavorite(note.id);
                      }}
                      className="hover:scale-110 transition-transform p-1"
                    >
                      <i className={`fas fa-star text-sm lg:text-base ${note.isFavorite ? 'text-yellow-500' : 'text-text-secondary'}`}></i>
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteNote(note.id);
                      }}
                      className="text-red-500 hover:text-red-700 p-1"
                    >
                      <i className="fas fa-trash text-sm lg:text-base"></i>
                    </button>
                  </div>
                </div>
                <div className="text-text-secondary text-sm lg:text-sm mb-4 line-clamp-4 lg:line-clamp-3">
                  {note.content.split('\n').slice(0, 4).map((line, i) => (
                    <p key={i} className="leading-relaxed">{line || '\u00A0'}</p>
                  ))}
                </div>
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex flex-wrap gap-2">
                    {note.tags.slice(0, 3).map(tag => (
                      <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                        #{tag}
                      </span>
                    ))}
                  </div>
                  <span className={`text-xs px-2 py-1 rounded whitespace-nowrap ${
                    note.priority === 'high' ? 'bg-red-100 text-red-700' :
                    note.priority === 'medium' ? 'bg-yellow-100 text-yellow-700' :
                    'bg-green-100 text-green-700'
                  }`}>
                    {note.priority}
                  </span>
                </div>
                {note.subfolder && (
                  <div className="mt-3 text-xs text-text-secondary truncate">
                    <i className="fas fa-folder-tree mr-1"></i>
                    {note.folder}/{note.subfolder}
                  </div>
                )}
              </div>
            ))}
          </div>

          {filteredNotes.length === 0 && (
            <div className="text-center py-16">
              <i className="fas fa-note-sticky text-6xl text-gray-300 mb-4"></i>
              <p className="text-gray-500 text-lg">No notes found</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
