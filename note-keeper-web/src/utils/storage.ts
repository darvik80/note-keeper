import { Settings, Theme } from '../types';

const SETTINGS_KEY = 'notekeeper_settings';
const THEME_KEY = 'notekeeper_theme';

export const storage = {
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

  saveSettings: (settings: Settings): void => {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  },

  getTheme: (): Theme => {
    const theme = localStorage.getItem(THEME_KEY);
    return (theme as Theme) || 'light';
  },

  saveTheme: (theme: Theme): void => {
    localStorage.setItem(THEME_KEY, theme);
  }
};
