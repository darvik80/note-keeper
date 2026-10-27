/**
 * @module Header
 * @category Components
 * Editorial page header: large title, quiet meta, actions, theme, account.
 */
import React, {useEffect, useRef, useState} from 'react';
import {ThemeSelector} from './ThemeSelector';
import {openCommandPalette} from './CommandPalette';

interface User {
  id: string;
  email: string;
  name: string;
  picture?: string;
}

interface HeaderProps {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}

const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);

export const Header: React.FC<HeaderProps> = ({ title, subtitle, actions }) => {
  const [user, setUser] = useState<User | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const loadUser = () => {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        setUser(JSON.parse(userStr));
      }
    };

    loadUser();

    const handleStorageChange = () => loadUser();
    window.addEventListener('storage', handleStorageChange);

    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };

    if (menuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [menuOpen]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.hash = '#/login';
    window.location.reload();
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(part => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div className="px-5 sm:px-8 lg:px-10 pt-5 sm:pt-7 pb-4 flex flex-wrap items-end justify-between gap-4 min-w-0">
      <div className="min-w-0">
        <h1 className="text-hero text-text">{title}</h1>
        {subtitle && (
          <p className="mt-1.5 text-[15px] text-text-secondary">{subtitle}</p>
        )}
      </div>
      <div className="flex items-center gap-2 sm:gap-3 min-w-0 ml-auto">
        {actions && <div className="flex items-center gap-2 flex-wrap justify-end">{actions}</div>}
        <button
          type="button"
          onClick={openCommandPalette}
          className="hidden md:inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-border text-[13px] text-text-secondary hover:bg-hover transition-colors duration-[180ms]"
          aria-label="Open command palette"
        >
          <i className="fas fa-search text-xs" aria-hidden="true"></i>
          <span>Search</span>
          <kbd className="nk-kbd">{isMac ? '⌘K' : 'Ctrl+K'}</kbd>
        </button>
        <ThemeSelector />
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setMenuOpen(!menuOpen)}
            className="rounded-full focus-visible:outline-none"
            aria-label="User menu"
          >
            {user?.picture ? (
              <img
                src={user.picture}
                alt={user.name}
                className="w-8 h-8 sm:w-9 sm:h-9 rounded-full object-cover"
              />
            ) : (
              <div className="w-8 h-8 sm:w-9 sm:h-9 rounded-full bg-hover text-text flex items-center justify-center font-semibold text-xs">
                {user ? getInitials(user.name) : 'U'}
              </div>
            )}
          </button>

          {menuOpen && user && (
            <div className="absolute right-0 top-full mt-2 w-64 nk-card z-50 overflow-hidden nk-fade">
              <div className="p-4 border-b border-border">
                <div className="flex items-center gap-3">
                  {user.picture ? (
                    <img
                      src={user.picture}
                      alt={user.name}
                      className="w-11 h-11 rounded-full object-cover shrink-0"
                    />
                  ) : (
                    <div className="w-11 h-11 rounded-full bg-hover flex items-center justify-center text-text font-semibold shrink-0">
                      {getInitials(user.name)}
                    </div>
                  )}
                  <div className="min-w-0">
                    <p className="font-semibold text-text truncate">{user.name}</p>
                    <p className="text-xs text-text-secondary truncate">{user.email}</p>
                  </div>
                </div>
              </div>
              <div className="p-1.5">
                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-3 px-3 py-2.5 text-sm text-danger hover:bg-hover rounded-xl transition-colors duration-[180ms]"
                >
                  <i className="fas fa-sign-out-alt w-5 text-center"></i>
                  Sign out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
