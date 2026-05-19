/**
 * @module ShortcutContext
 * @category Contexts
 * React context for global keyboard shortcut handling.
 *
 * {@link ShortcutProvider} attaches a single `keydown` listener to `window`
 * and interprets key combos according to the user's persisted
 * {@link Settings.shortcuts} bindings.  Built-in actions:
 *
 * | Default binding | Action                                   |
 * |-----------------|------------------------------------------|
 * | `Ctrl+N`        | Create a new note and navigate to it     |
 * | `Ctrl+T`        | Create a new todo and navigate to it     |
 * | `Ctrl+K`        | Navigate to the Search page              |
 * | `Ctrl+B`        | Dispatch `"toggle-sidebar"` custom event |
 *
 * Wrap the router with {@link ShortcutProvider} and access the context via
 * {@link useShortcuts}.
 */
import React, { createContext, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { storage } from '../utils/storage';
import { api } from '../utils/api';

/**
 * Shape of the value exposed by {@link ShortcutContext}.
 */
interface ShortcutContextType {
  /**
   * Register a custom shortcut handler at runtime.
   *
   * > **Note:** This is currently a stub — the `callback` is not invoked.
   * > Override behaviour is not yet implemented.
   *
   * @param key - Key combo string (e.g. `"ctrl+shift+p"`).
   * @param callback - Function to call when the combo is pressed.
   */
  registerShortcut: (key: string, callback: () => void) => void;
}

/**
 * Internal React context — use {@link useShortcuts} instead.
 */
const ShortcutContext = createContext<ShortcutContextType | undefined>(undefined);

/**
 * Provider that installs global keyboard shortcut handling for the application.
 *
 * Must be placed **inside** a React Router provider so that `useNavigate` is
 * available.
 *
 * @param children - Child component tree.
 *
 * @example
 * ```tsx
 * // App.tsx
 * <HashRouter>
 *   <ShortcutProvider>
 *     <Layout />
 *   </ShortcutProvider>
 * </HashRouter>
 * ```
 */
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
        }
      } else if (key === shortcuts.search) {
        e.preventDefault();
        navigate('/search');
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

/**
 * Hook to access the shortcut registration API.
 *
 * **Must** be called inside a {@link ShortcutProvider}.
 *
 * @returns `{ registerShortcut }` from the nearest {@link ShortcutProvider}.
 */
export const useShortcuts = () => {
  const context = useContext(ShortcutContext);
  if (!context) {
    throw new Error('useShortcuts must be used within ShortcutProvider');
  }
  return context;
};
