/**
 * @module storage
 * @category Utils
 * Typed wrappers around `localStorage` for persisting user settings and theme
 * preference between sessions.
 *
 * All reads are null-safe: missing or corrupted keys return sensible defaults.
 */
import { Settings, Theme } from '../types';

/** `localStorage` key for the full {@link Settings} object. */
const SETTINGS_KEY = 'notekeeper_settings';

/** `localStorage` key for the active {@link Theme} name. */
const THEME_KEY = 'notekeeper_theme';

/**
 * Typed `localStorage` client for NoteKeeper user preferences.
 *
 * @example
 * ```ts
 * import { storage } from '../utils/storage';
 *
 * const settings = storage.getSettings();
 * settings.theme = 'dark';
 * storage.saveSettings(settings);
 * ```
 */
export const storage = {
  /**
   * Read the persisted {@link Settings} from `localStorage`.
   *
   * If no settings have been saved yet, or any top-level key is missing,
   * the returned object is merged with a safe default so callers can always
   * destructure every field without null checks.
   *
   * **Default shortcut bindings:**
   * | Action         | Default key  |
   * |----------------|--------------|
   * | New note       | `Ctrl+N`     |
   * | New todo       | `Ctrl+T`     |
   * | Search         | `Ctrl+K`     |
   * | Toggle sidebar | `Ctrl+B`     |
   *
   * @returns Merged {@link Settings} object — never `null`.
   */
  getSettings: (): Settings => {
    const data = localStorage.getItem(SETTINGS_KEY);
    const defaultSettings = {
      telegram: { enabled: false, botToken: '', chatId: '' },
      dingtalk: { enabled: false, webhook: '', secret: '' },
      email: { enabled: false, smtp: '', port: 587, username: '', password: '', from: '', to: '' },
      theme: 'light',
      shortcuts: {
        newNote: 'ctrl+n',
        newTodo: 'ctrl+t',
        search: 'ctrl+k',
        toggleSidebar: 'ctrl+b'
      }
    };

    if (!data) return defaultSettings;

    const parsed = JSON.parse(data);
    return {
      ...defaultSettings,
      ...parsed,
      email: parsed.email || defaultSettings.email,
      shortcuts: parsed.shortcuts || defaultSettings.shortcuts
    };
  },

  /**
   * Persist the full {@link Settings} object to `localStorage`.
   * Serialises the object as JSON; previous value is overwritten.
   *
   * @param settings - The settings object to save.
   */
  saveSettings: (settings: Settings): void => {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  },

  /**
   * Read the persisted {@link Theme} name from `localStorage`.
   * Falls back to `"light"` when no theme has been saved.
   *
   * @returns The active theme name.
   */
  getTheme: (): Theme => {
    const theme = localStorage.getItem(THEME_KEY);
    return (theme as Theme) || 'light';
  },

  /**
   * Persist the active {@link Theme} name to `localStorage`.
   *
   * @param theme - Theme identifier to save.
   */
  saveTheme: (theme: Theme): void => {
    localStorage.setItem(THEME_KEY, theme);
  }
};
