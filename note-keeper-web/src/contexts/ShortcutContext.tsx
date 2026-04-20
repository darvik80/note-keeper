import React, { createContext, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { storage } from '../utils/storage';
import { api } from '../utils/api';

interface ShortcutContextType {
  registerShortcut: (key: string, callback: () => void) => void;
}

const ShortcutContext = createContext<ShortcutContextType | undefined>(undefined);

export const ShortcutProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();

  useEffect(() => {
    const settings = storage.getSettings();
    const shortcuts = settings?.shortcuts;

    const handleKeyDown = async (e: KeyboardEvent) => {
      if (!e.key) return;
      const key = `${e.ctrlKey ? 'ctrl+' : ''}${e.shiftKey ? 'shift+' : ''}${e.altKey ? 'alt+' : ''}${e.key.toLowerCase()}`;

      if (!shortcuts) return;

      if (key === shortcuts.newNote) {
        e.preventDefault();
        try {
          const newNote = await api.notes.create({
            title: 'Untitled Note',
            content: '',
            tags: [],
            folder: 'default',
            priority: 'medium',
            isFavorite: false,
            isEncrypted: false,
          });
          navigate(`/notes/${newNote.id}`);
        } catch (err) {
          console.error('Failed to create note via shortcut', err);
        }
      } else if (key === shortcuts.newTodo) {
        e.preventDefault();
        try {
          const newTodo = await api.todos.create({
            title: 'New Todo',
            description: '',
            tags: [],
            priority: 'medium',
            isFavorite: false,
            completed: false,
          });
          navigate(`/todos/${newTodo.id}`);
        } catch (err) {
          console.error('Failed to create todo via shortcut', err);
        }
      } else if (key === shortcuts.search) {
        e.preventDefault();
        navigate('/search');
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [navigate]);

  const registerShortcut = (key: string, callback: () => void) => {
    console.log('Shortcut registered:', key);
  };

  return (
    <ShortcutContext.Provider value={{ registerShortcut }}>
      {children}
    </ShortcutContext.Provider>
  );
};

export const useShortcuts = () => {
  const context = useContext(ShortcutContext);
  if (!context) {
    throw new Error('useShortcuts must be used within ShortcutProvider');
  }
  return context;
};
