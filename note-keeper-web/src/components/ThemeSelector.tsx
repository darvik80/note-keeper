/**
 * @module ThemeSelector
 * @category Components
 * Theme picker with colour swatches.
 */
import React, {useEffect, useRef, useState} from 'react';
import {Theme} from '../types';
import {themes} from '../utils/themes';
import {useTheme} from '../contexts/ThemeContext';

const ORDER: Theme[] = [
  'cogitor', 'light', 'dark', 'darcula', 'slate',
  'green', 'teal', 'cyan', 'blue', 'indigo', 'purple', 'rose', 'amber',
];

export const ThemeSelector: React.FC = () => {
  const { theme, setTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const current = themes[theme];

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 p-1.5 sm:pl-1.5 sm:pr-3 sm:py-1.5 rounded-full border border-border hover:bg-hover transition-colors duration-[180ms]"
        title={current.name}
        aria-label={`Theme: ${current.name}`}
      >
        <span
          className="w-5 h-5 rounded-full border border-border shrink-0"
          style={{ background: current.colors.primary }}
        />
        <span className="hidden sm:inline text-[13px] text-text font-medium">{current.name}</span>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 nk-card z-50 p-3 nk-fade">
          <p className="px-1 pb-2 text-[11px] font-medium uppercase tracking-wider text-text-secondary">
            Appearance
          </p>
          <div className="grid grid-cols-1 gap-0.5 max-h-80 overflow-y-auto">
            {ORDER.map(id => {
              const option = themes[id];
              const selected = theme === id;
              return (
                <button
                  key={id}
                  onClick={() => {
                    setTheme(id);
                    setIsOpen(false);
                  }}
                  className={`w-full flex items-center gap-3 px-2 py-2 rounded-xl text-left transition-colors duration-[180ms] ${
                    selected ? 'bg-hover' : 'hover:bg-hover'
                  }`}
                >
                  <span className="flex h-7 w-7 rounded-full overflow-hidden border border-border shrink-0">
                    <span className="w-1/2 h-full" style={{ background: option.colors.background }} />
                    <span className="w-1/2 h-full" style={{ background: option.colors.primary }} />
                  </span>
                  <span className={`flex-1 text-[14px] ${selected ? 'text-primary font-medium' : 'text-text'}`}>
                    {option.name}
                  </span>
                  {selected && <i className="fas fa-check text-primary text-xs" aria-hidden="true"></i>}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
