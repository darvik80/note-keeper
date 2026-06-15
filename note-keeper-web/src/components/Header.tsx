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
import React, { useState, useEffect, useRef } from 'react';
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

  // Close menu when clicking outside
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
    <div className="bg-surface border-b border-border px-4 sm:px-8 py-3 sm:py-4 flex items-center justify-between gap-2 min-w-0">
      <h2 className="text-xl sm:text-2xl font-bold text-text shrink-0">{title}</h2>
      <div className="flex items-center gap-2 sm:gap-4 min-w-0">
        {actions && <div className="flex items-center gap-2 sm:gap-3 shrink-0">{actions}</div>}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <ThemeSelector />
          <button className="p-2 hover:bg-hover rounded-lg transition-colors">
            <i className="fas fa-bell text-text-secondary"></i>
          </button>
          <div className="relative" ref={menuRef}>
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              className="focus:outline-none focus:ring-2 focus:ring-primary rounded-full"
              aria-label="User menu"
            >
              {user?.picture ? (
                <img
                  src={user.picture}
                  alt={user.name}
                  className="w-8 h-8 sm:w-10 sm:h-10 rounded-full object-cover cursor-pointer hover:opacity-90 transition-opacity"
                />
              ) : (
                <div className="w-8 h-8 sm:w-10 sm:h-10 bg-gradient-to-br from-primary to-secondary rounded-full flex items-center justify-center text-white font-bold text-sm cursor-pointer hover:opacity-90 transition-opacity">
                  {user ? getInitials(user.name) : 'U'}
                </div>
              )}
            </button>

            {menuOpen && user && (
              <div className="absolute right-0 top-full mt-2 w-64 bg-surface border border-border rounded-xl shadow-lg z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                {/* User info section */}
                <div className="p-4 border-b border-border">
                  <div className="flex items-center gap-3">
                    {user.picture ? (
                      <img
                        src={user.picture}
                        alt={user.name}
                        className="w-12 h-12 rounded-full object-cover shrink-0"
                      />
                    ) : (
                      <div className="w-12 h-12 bg-gradient-to-br from-primary to-secondary rounded-full flex items-center justify-center text-white font-bold text-lg shrink-0">
                        {getInitials(user.name)}
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="font-semibold text-text truncate">{user.name}</p>
                      <p className="text-xs text-text-secondary truncate">{user.email}</p>
                    </div>
                  </div>
                </div>

                {/* Menu actions */}
                <div className="p-1">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-3 py-2.5 text-sm text-red-500 hover:bg-hover rounded-lg transition-colors"
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
    </div>
  );
};
