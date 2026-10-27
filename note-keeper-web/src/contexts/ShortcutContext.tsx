/**
 * @module ShortcutContext
 * @category Contexts
 * Global keyboard shortcuts. Search opens the command palette.
 */
import React, {createContext, useContext, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {storage} from '../utils/storage';
import {api} from '../utils/api';

interface ShortcutContextType {
  registerShortcut: (key: string, callback: () => void) => void;
}

const ShortcutContext = createContext<ShortcutContextType | undefined>(undefined);

const isTypingTarget = (target: EventTarget | null) => {
  if (!(target instanceof HTMLElement)) return false;
  const tag = target.tagName;
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || target.isContentEditable;
};

export const ShortcutProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const navigate = useNavigate();

  useEffect(() => {
    const settings = storage.getSettings();
    const shortcuts = settings?.shortcuts;

    const handleKeyDown = async (e: KeyboardEvent) => {
      if (!e.key || !shortcuts) return;
      const modifier = e.metaKey || e.ctrlKey;
      const key = `${modifier ? 'ctrl+' : ''}${e.shiftKey ? 'shift+' : ''}${e.altKey ? 'alt+' : ''}${e.key.toLowerCase()}`;

      if (key === shortcuts.search) {
        e.preventDefault();
        window.dispatchEvent(new CustomEvent('open-command-palette'));
        return;
      }

      if (isTypingTarget(e.target)) return;

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
        } catch {
          /* ignore */
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
        } catch {
          /* ignore */
        }
      } else if (key === shortcuts.toggleSidebar) {
        e.preventDefault();
        window.dispatchEvent(new CustomEvent('toggle-sidebar'));
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
