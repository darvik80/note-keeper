/**
 * @module themes
 * @category Utils
 * Theme colour palette definitions and DOM application utility.
 *
 * All 12 themes expose the same eight semantic colour tokens, which are written
 * to CSS custom properties on `:root` by {@link applyTheme}.  Tailwind classes
 * (`text-primary`, `bg-surface`, etc.) resolve to these custom properties via
 * `tailwind.config.js`.
 */
import { Theme } from '../types';

/**
 * Map of all supported themes to their metadata and colour values.
 *
 * Each theme exposes eight semantic tokens:
 * | Token           | CSS variable               | Tailwind class           |
 * |-----------------|----------------------------|--------------------------|
 * | `primary`       | `--color-primary`          | `text-primary` / `bg-primary` |
 * | `secondary`     | `--color-secondary`        | `text-secondary`         |
 * | `background`    | `--color-background`       | `bg-background`          |
 * | `surface`       | `--color-surface`          | `bg-surface`             |
 * | `text`          | `--color-text`             | `text-text`              |
 * | `textSecondary` | `--color-text-secondary`   | `text-text-secondary`    |
 * | `border`        | `--color-border`           | `border-border`          |
 * | `hover`         | `--color-hover`            | `hover:bg-hover`         |
 */
export const themes: Record<Theme, {
  /** Human-readable display name shown in the {@link ThemeSelector}. */
  name: string;
  /** Colour palette for this theme. */
  colors: {
    /** Brand / accent colour used for buttons and highlights. */
    primary: string;
    /** Secondary accent colour. */
    secondary: string;
    /** Page background colour. */
    background: string;
    /** Card / panel surface colour (usually white or a slightly lighter shade). */
    surface: string;
    /** Primary text colour. */
    text: string;
    /** Muted / secondary text colour. */
    textSecondary: string;
    /** Border and divider colour. */
    border: string;
    /** Hover state background colour. */
    hover: string;
  };
}> = {
  light: {
    name: 'Light',
    colors: {
      primary: '#2ecc71',
      secondary: '#3498db',
      background: '#f5f7fa',
      surface: '#ffffff',
      text: '#2c3e50',
      textSecondary: '#7f8c8d',
      border: '#e1e8ed',
      hover: '#ecf0f1'
    }
  },
  dark: {
    name: 'Dark',
    colors: {
      primary: '#2ecc71',
      secondary: '#3498db',
      background: '#1a1a1a',
      surface: '#2d2d2d',
      text: '#e0e0e0',
      textSecondary: '#a0a0a0',
      border: '#404040',
      hover: '#3a3a3a'
    }
  },
  green: {
    name: 'Green',
    colors: {
      primary: '#27ae60',
      secondary: '#16a085',
      background: '#e8f5e9',
      surface: '#ffffff',
      text: '#1b5e20',
      textSecondary: '#558b2f',
      border: '#c8e6c9',
      hover: '#f1f8e9'
    }
  },
  cyan: {
    name: 'Cyan',
    colors: {
      primary: '#00bcd4',
      secondary: '#0097a7',
      background: '#e0f7fa',
      surface: '#ffffff',
      text: '#006064',
      textSecondary: '#00838f',
      border: '#b2ebf2',
      hover: '#e0f2f1'
    }
  },
  blue: {
    name: 'Blue',
    colors: {
      primary: '#2196f3',
      secondary: '#1976d2',
      background: '#e3f2fd',
      surface: '#ffffff',
      text: '#0d47a1',
      textSecondary: '#1565c0',
      border: '#bbdefb',
      hover: '#e8eaf6'
    }
  },
  purple: {
    name: 'Purple',
    colors: {
      primary: '#9c27b0',
      secondary: '#7b1fa2',
      background: '#f3e5f5',
      surface: '#ffffff',
      text: '#4a148c',
      textSecondary: '#6a1b9a',
      border: '#e1bee7',
      hover: '#ede7f6'
    }
  },
  darcula: {
    name: 'Darcula',
    colors: {
      primary: '#cc7832',
      secondary: '#6897bb',
      background: '#2b2b2b',
      surface: '#3c3f41',
      text: '#a9b7c6',
      textSecondary: '#808080',
      border: '#555555',
      hover: '#4b4d4f'
    }
  },
  rose: {
    name: 'Rose',
    colors: {
      primary: '#e91e63',
      secondary: '#c2185b',
      background: '#fce4ec',
      surface: '#ffffff',
      text: '#880e4f',
      textSecondary: '#ad1457',
      border: '#f8bbd0',
      hover: '#f3e5f5'
    }
  },
  amber: {
    name: 'Amber',
    colors: {
      primary: '#ff9800',
      secondary: '#f57c00',
      background: '#fff3e0',
      surface: '#ffffff',
      text: '#e65100',
      textSecondary: '#ef6c00',
      border: '#ffe0b2',
      hover: '#fff8e1'
    }
  },
  teal: {
    name: 'Teal',
    colors: {
      primary: '#009688',
      secondary: '#00796b',
      background: '#e0f2f1',
      surface: '#ffffff',
      text: '#004d40',
      textSecondary: '#00695c',
      border: '#b2dfdb',
      hover: '#e0f7fa'
    }
  },
  indigo: {
    name: 'Indigo',
    colors: {
      primary: '#3f51b5',
      secondary: '#303f9f',
      background: '#e8eaf6',
      surface: '#ffffff',
      text: '#1a237e',
      textSecondary: '#283593',
      border: '#c5cae9',
      hover: '#ede7f6'
    }
  },
  slate: {
    name: 'Slate',
    colors: {
      primary: '#607d8b',
      secondary: '#455a64',
      background: '#eceff1',
      surface: '#ffffff',
      text: '#263238',
      textSecondary: '#37474f',
      border: '#cfd8dc',
      hover: '#f5f5f5'
    }
  }
};

/**
 * Apply a theme by writing its colour values to CSS custom properties on
 * `document.documentElement` (`:root`) and updating `document.body` styles.
 *
 * Called automatically by {@link ThemeContext.ThemeProvider} when the theme
 * changes, and also on initial mount to restore the persisted theme.
 *
 * @param theme - The {@link Theme} identifier to activate.
 *
 * @example
 * ```ts
 * import { applyTheme } from '../utils/themes';
 * applyTheme('dark');
 * ```
 */
export const applyTheme = (theme: Theme) => {
  const themeColors = themes[theme].colors;
  const root = document.documentElement;

  root.style.setProperty('--color-primary', themeColors.primary);
  root.style.setProperty('--color-secondary', themeColors.secondary);
  root.style.setProperty('--color-background', themeColors.background);
  root.style.setProperty('--color-surface', themeColors.surface);
  root.style.setProperty('--color-text', themeColors.text);
  root.style.setProperty('--color-text-secondary', themeColors.textSecondary);
  root.style.setProperty('--color-border', themeColors.border);
  root.style.setProperty('--color-hover', themeColors.hover);

  document.body.style.background = themeColors.background;
  document.body.style.color = themeColors.text;
};
