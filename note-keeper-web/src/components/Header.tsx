import React, { useState, useEffect } from 'react';
import { ThemeSelector } from './ThemeSelector';

interface User {
  id: string;
  email: string;
  name: string;
  picture?: string;
}

interface HeaderProps {
  title: string;
  actions?: React.ReactNode;
}

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
    <div className="bg-surface border-b border-border px-8 py-4 flex items-center justify-between">
      <h2 className="text-2xl font-bold text-text">{title}</h2>
      <div className="flex items-center gap-4">
        {actions}
        <div className="flex items-center gap-3">
          <ThemeSelector />
          <button className="p-2 hover:bg-hover rounded-lg transition-colors">
            <i className="fas fa-bell text-text-secondary"></i>
          </button>
          {user?.picture ? (
            <img
              src={user.picture}
              alt={user.name}
              className="w-10 h-10 rounded-full object-cover"
            />
          ) : (
            <div className="w-10 h-10 bg-gradient-to-br from-primary to-secondary rounded-full flex items-center justify-center text-white font-bold">
              {user ? getInitials(user.name) : 'U'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
