/**
 * @module Sidebar
 * @category Components
 * Quiet navigation. Desktop rail; mobile drawer.
 */
import React, {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {CogitorMark} from './CogitorMark';
import {openCommandPalette} from './CommandPalette';

interface SidebarProps {
  isMobileMenuOpen: boolean;
  setIsMobileMenuOpen: (open: boolean) => void;
}

const GROUPS: { label?: string; items: { path: string; icon: string; label: string }[] }[] = [
  {
    items: [
      { path: '/', icon: 'fa-house', label: 'Home' },
      { path: '/notes', icon: 'fa-note-sticky', label: 'Notes' },
      { path: '/todos', icon: 'fa-list-check', label: 'Todos' },
      { path: '/vault', icon: 'fa-vault', label: 'Vault' },
    ],
  },
  {
    label: 'Find',
    items: [
      { path: '/search', icon: 'fa-magnifying-glass', label: 'Search' },
      { path: '/calendar', icon: 'fa-calendar', label: 'Calendar' },
      { path: '/analytics', icon: 'fa-chart-line', label: 'Analytics' },
    ],
  },
  {
    label: 'Library',
    items: [
      { path: '/favorites', icon: 'fa-star', label: 'Favorites' },
      { path: '/templates', icon: 'fa-file-lines', label: 'Templates' },
      { path: '/archive', icon: 'fa-box-archive', label: 'Archive' },
      { path: '/trash', icon: 'fa-trash', label: 'Trash' },
    ],
  },
];

const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);

export const Sidebar: React.FC<SidebarProps> = ({ isMobileMenuOpen, setIsMobileMenuOpen }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleNavigate = (path: string) => {
    navigate(path);
    setIsMobileMenuOpen(false);
  };

  const isActive = (path: string) =>
    path === '/'
      ? location.pathname === '/'
      : location.pathname === path || location.pathname.startsWith(`${path}/`);

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
        w-[240px] bg-background lg:bg-transparent h-screen flex flex-col
        transform transition-transform duration-[180ms] ease-out
        ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}>
        <div className="px-5 pt-6 pb-4 flex items-center justify-between">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-9 h-9 rounded-[10px] bg-[#0B0E11] flex items-center justify-center shrink-0 text-[#EDEFF2]">
              <CogitorMark size={22} />
            </div>
            <span className="text-[17px] font-semibold tracking-tight text-text truncate">NoteKeeper</span>
          </div>
          <button
            onClick={() => setIsMobileMenuOpen(false)}
            className="lg:hidden p-2 text-text-secondary hover:text-text hover:bg-hover rounded-xl transition-colors duration-[180ms]"
            aria-label="Close menu"
          >
            <i className="fas fa-times"></i>
          </button>
        </div>

        <nav className="flex-1 px-3 overflow-y-auto pb-4">
          {GROUPS.map((group, gi) => (
            <div key={gi} className={gi > 0 ? 'mt-5' : ''}>
              {group.label && (
                <p className="px-3 mb-1 text-[11px] font-medium uppercase tracking-wider text-text-secondary">
                  {group.label}
                </p>
              )}
              {group.items.map(item => {
                const active = isActive(item.path);
                return (
                  <button
                    key={item.path}
                    onClick={() => handleNavigate(item.path)}
                    className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl mb-0.5 text-[15px] transition-colors duration-[180ms] ${
                      active
                        ? 'bg-primary/15 text-primary'
                        : 'text-text-secondary hover:text-text hover:bg-hover'
                    }`}
                  >
                    <i className={`fas ${item.icon} w-4 text-center text-sm`}></i>
                    <span className={active ? 'font-medium' : ''}>{item.label}</span>
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="px-3 pb-4 space-y-1">
          <button
            type="button"
            onClick={() => {
              setIsMobileMenuOpen(false);
              openCommandPalette();
            }}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-[15px] text-text-secondary hover:text-text hover:bg-hover transition-colors duration-[180ms]"
          >
            <i className="fas fa-search w-4 text-center text-sm"></i>
            <span className="flex-1 text-left">Commands</span>
            <kbd className="nk-kbd">{isMac ? '⌘K' : 'Ctrl+K'}</kbd>
          </button>
          <button
            onClick={() => handleNavigate('/settings')}
            className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-[15px] transition-colors duration-[180ms] ${
              isActive('/settings')
                ? 'bg-primary/15 text-primary'
                : 'text-text-secondary hover:text-text hover:bg-hover'
            }`}
          >
            <i className="fas fa-gear w-4 text-center text-sm"></i>
            <span>Settings</span>
          </button>
        </div>
      </div>
    </>
  );
};
