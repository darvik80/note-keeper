/**
 * @module ThemeSelector
 * @category Components
 * Dropdown button for switching the active UI theme.
 *
 * On mobile the button shows only the theme icon; on `sm` and wider it also
 * shows the theme name.  The dropdown is scrollable and lists all 12 themes.
 * Clicking outside closes the dropdown via a `mousedown` listener.
 *
 * Delegates theme persistence and CSS application to {@link useTheme}.
 */
import React, {useEffect, useRef, useState} from 'react';
import {Theme} from '../types';
import {themes} from '../utils/themes';
import {useTheme} from '../contexts/ThemeContext';

/**
 * Theme picker dropdown.  No props required — reads and updates via {@link useTheme}.
 */
export const ThemeSelector: React.FC = () => {
  const { theme, setTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const themeOptions: { value: Theme; icon: string }[] = [
    { value: 'light', icon: 'fa-sun' },
    { value: 'dark', icon: 'fa-moon' },
    { value: 'green', icon: 'fa-leaf' },
    { value: 'cyan', icon: 'fa-water' },
    { value: 'blue', icon: 'fa-cloud' },
    { value: 'purple', icon: 'fa-star' },
    { value: 'darcula', icon: 'fa-code' },
    { value: 'rose', icon: 'fa-heart' },
    { value: 'amber', icon: 'fa-fire' },
    { value: 'teal', icon: 'fa-spa' },
    { value: 'indigo', icon: 'fa-gem' },
    { value: 'slate', icon: 'fa-circle-half-stroke' },
  ];

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentTheme = themeOptions.find(opt => opt.value === theme);

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-1 sm:gap-2 p-2 sm:px-4 sm:py-2 bg-surface border border-border rounded-lg hover:bg-hover transition-colors"
        title={themes[theme].name}
      >
        <i className={`fas ${currentTheme?.icon} text-primary`}></i>
        <span className="hidden sm:inline text-text font-medium">{themes[theme].name}</span>
        <i className={`fas fa-chevron-${isOpen ? 'up' : 'down'} text-text-secondary text-xs`}></i>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-52 bg-surface border border-border rounded-lg shadow-lg overflow-y-auto z-50 max-h-80">
          {themeOptions.map(option => (
            <button
              key={option.value}
              onClick={() => {
                setTheme(option.value);
                setIsOpen(false);
              }}
              className={`w-full flex items-center gap-3 px-4 py-3 hover:bg-hover transition-colors ${
                theme === option.value ? 'bg-primary/10' : ''
              }`}
            >
              <i className={`fas ${option.icon} ${theme === option.value ? 'text-primary' : 'text-text-secondary'}`}></i>
              <span className={`font-medium ${theme === option.value ? 'text-primary' : 'text-text'}`}>
                {themes[option.value].name}
              </span>
              {theme === option.value && (
                <i className="fas fa-check text-primary ml-auto"></i>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
