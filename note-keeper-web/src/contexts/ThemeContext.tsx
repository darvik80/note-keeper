/**
 * @module ThemeContext
 * @category Contexts
 * React context for application-wide theme management.
 *
 * Wrap the component tree with {@link ThemeProvider} once (in `App.tsx`) and
 * consume the active theme or the setter anywhere with {@link useTheme}.
 *
 * On mount, the provider restores the last persisted theme from `localStorage`
 * via {@link storage.getTheme} and applies it to the DOM immediately via
 * {@link applyTheme}.
 */
import React, {createContext, useContext, useEffect, useState} from 'react';
import {Theme} from '../types';
import {storage} from '../utils/storage';
import {applyTheme} from '../utils/themes';

/**
 * Shape of the value exposed by {@link ThemeContext}.
 */
interface ThemeContextType {
  /** The currently active theme identifier. */
  theme: Theme;
  /**
   * Change the active theme, persist it to `localStorage`, and apply CSS
   * custom properties immediately.
   *
   * @param theme - New theme to activate.
   */
  setTheme: (theme: Theme) => void;
}

/**
 * Internal React context — use the {@link useTheme} hook instead of consuming
 * this directly.
 */
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

/**
 * Provider that manages the active {@link Theme} and keeps it in sync with
 * the DOM and `localStorage`.
 *
 * @param children - Child component tree.
 *
 * @example
 * ```tsx
 * // App.tsx
 * <ThemeProvider>
 *   <Router>…</Router>
 * </ThemeProvider>
 * ```
 */
export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setThemeState] = useState<Theme>(storage.getTheme());

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  const setTheme = (newTheme: Theme) => {
    setThemeState(newTheme);
    storage.saveTheme(newTheme);
    applyTheme(newTheme);
  };

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

/**
 * Hook to read and update the active UI theme.
 *
 * **Must** be used inside a {@link ThemeProvider} — throws if called outside.
 *
 * @returns `{ theme, setTheme }` from the nearest {@link ThemeProvider}.
 *
 * @example
 * ```tsx
 * const { theme, setTheme } = useTheme();
 * setTheme('dark');
 * ```
 */
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
};
