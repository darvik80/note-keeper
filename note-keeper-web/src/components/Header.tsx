import React from 'react';
import { ThemeSelector } from './ThemeSelector';

interface HeaderProps {
  title: string;
  actions?: React.ReactNode;
}

export const Header: React.FC<HeaderProps> = ({ title, actions }) => {
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
          <div className="w-10 h-10 bg-gradient-to-br from-primary to-secondary rounded-full flex items-center justify-center text-white font-bold">
            IK
          </div>
        </div>
      </div>
    </div>
  );
};
