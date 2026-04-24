import React, { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { api } from '../utils/api';

interface SidebarProps {
  isMobileMenuOpen: boolean;
  setIsMobileMenuOpen: (open: boolean) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isMobileMenuOpen, setIsMobileMenuOpen }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    { path: '/', icon: 'fa-home', label: 'Dashboard' },
    { path: '/notes', icon: 'fa-note-sticky', label: 'Notes' },
    { path: '/todos', icon: 'fa-list-check', label: 'Todos' },
    { path: '/search', icon: 'fa-search', label: 'Search' },
    { path: '/calendar', icon: 'fa-calendar', label: 'Calendar' },
    { path: '/analytics', icon: 'fa-chart-line', label: 'Analytics' },
    { path: '/favorites', icon: 'fa-star', label: 'Favorites' },
    { path: '/templates', icon: 'fa-file-lines', label: 'Templates' },
    { path: '/archive', icon: 'fa-box-archive', label: 'Archive' },
    { path: '/trash', icon: 'fa-trash', label: 'Trash' },
    { path: '/settings', icon: 'fa-gear', label: 'Settings' }
  ];

  const handleNavigate = (path: string) => {
    navigate(path);
    setIsMobileMenuOpen(false);
  };

  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isMobileMenuOpen]);

  // Listen for toggle-sidebar event from shortcuts
  useEffect(() => {
    const handleToggleSidebar = () => {
      setIsMobileMenuOpen(!isMobileMenuOpen);
    };
    window.addEventListener('toggle-sidebar', handleToggleSidebar);
    return () => window.removeEventListener('toggle-sidebar', handleToggleSidebar);
  }, [isMobileMenuOpen, setIsMobileMenuOpen]);

  return (
    <>
      {isMobileMenuOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setIsMobileMenuOpen(false)}
        ></div>
      )}

      <div className={`
        fixed lg:static inset-y-0 left-0 z-50
        w-64 bg-surface border-r border-border h-screen flex flex-col
        transform transition-transform duration-300 ease-in-out
        ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}>
        <div className="p-6 border-b border-border flex items-center justify-between">
          <h1 className="text-2xl font-bold text-primary flex items-center gap-2">
            <i className="fas fa-book"></i>
            NoteKeeper
          </h1>
          <button
            onClick={() => setIsMobileMenuOpen(false)}
            className="lg:hidden p-2 text-text hover:bg-hover rounded-lg transition-colors"
          >
            <i className="fas fa-times"></i>
          </button>
        </div>

        <nav className="flex-1 p-4 overflow-y-auto">
          {menuItems.map(item => (
            <button
              key={item.path}
              onClick={() => handleNavigate(item.path)}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg mb-2 transition-all active:scale-95 ${
                location.pathname === item.path
                  ? 'bg-primary text-white'
                  : 'text-text hover:bg-hover'
              }`}
            >
              <i className={`fas ${item.icon} w-5`}></i>
              <span className="font-medium">{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="p-4 border-t border-border">
          <div className="bg-gradient-to-r from-primary to-secondary text-white p-4 rounded-lg mb-3">
            <p className="text-sm font-medium mb-1">Storage Used</p>
            <div className="flex items-center gap-2">
              <div className="flex-1 bg-white/30 rounded-full h-2">
                <div className="bg-white h-2 rounded-full" style={{ width: '45%' }}></div>
              </div>
              <span className="text-xs">45%</span>
            </div>
          </div>
          
          <button
            onClick={async () => {
              await api.auth.logout();
              navigate('/login');
            }}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-text hover:bg-hover transition-all active:scale-95"
          >
            <i className="fas fa-sign-out-alt w-5"></i>
            <span className="font-medium">Logout</span>
          </button>
        </div>
      </div>
    </>
  );
};
