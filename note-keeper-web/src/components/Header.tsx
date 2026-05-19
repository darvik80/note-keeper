/**
 * @module Header
 * @category Components
 * Top application bar shown on every page.
 *
 * Displays the page title, optional action buttons, the {@link ThemeSelector}
 * dropdown, a notification bell, and the current user's avatar.  User info is
 * read from `localStorage` (`"user"` key) and refreshed on cross-tab storage
 * events so the avatar updates immediately after login/logout in another tab.
 */
import React, { useState, useEffect } from 'react';
import { ThemeSelector } from './ThemeSelector';

/** Minimal user shape read from `localStorage`. */
interface User {
  id: string;
  email: string;
  name: string;
  /** Google profile picture URL, if available. */
  picture?: string;
}

/** Props for {@link Header}. */
interface HeaderProps {
  /** Page title displayed on the left side of the bar. */
  title: string;
  /**
   * Optional action buttons rendered between the title and the user controls.
   * Accepts any React node (typically `<button>` elements).
   */
  actions?: React.ReactNode;
}

/**
 * Top application bar.
 * @param props - See {@link HeaderProps}.
 */
export const Header: React.FC<HeaderProps> = ({ title, actions }) => {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const loadUser = () => {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        setUser(JSON.parse(userStr));
      }
    };

    loadUser();

    // Listen for storage changes (when user logs in/out in another tab)
    const handleStorageChange = () => loadUser();
    window.addEventListener('storage', handleStorageChange);

    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(part => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div className="bg-surface border-b border-border px-4 sm:px-8 py-3 sm:py-4 flex items-center justify-between gap-2 min-w-0">
      <h2 className="text-xl sm:text-2xl font-bold text-text shrink-0">{title}</h2>
      <div className="flex items-center gap-2 sm:gap-4 min-w-0">
        {actions && <div className="flex items-center gap-2 sm:gap-3 shrink-0">{actions}</div>}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <ThemeSelector />
          <button className="p-2 hover:bg-hover rounded-lg transition-colors">
            <i className="fas fa-bell text-text-secondary"></i>
          </button>
          {user?.picture ? (
            <img
              src={user.picture}
              alt={user.name}
              className="w-8 h-8 sm:w-10 sm:h-10 rounded-full object-cover"
            />
          ) : (
            <div className="w-8 h-8 sm:w-10 sm:h-10 bg-gradient-to-br from-primary to-secondary rounded-full flex items-center justify-center text-white font-bold text-sm">
              {user ? getInitials(user.name) : 'U'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
