import React, { Suspense, lazy, useState } from 'react';
import { HashRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { ThemeProvider } from './contexts/ThemeContext';
import { ToastProvider } from './contexts/ToastContext';
import { Sidebar } from './components/Sidebar';
import { PageLoader } from './components/PageLoader';
import { CommandPalette, openCommandPalette } from './components/CommandPalette';
import { CogitorMark } from './components/CogitorMark';
import Login from './pages/Login';

const Dashboard = lazy(() => import('./pages/Dashboard').then(m => ({ default: m.Dashboard })));
const Notes = lazy(() => import('./pages/Notes').then(m => ({ default: m.Notes })));
const NoteEditor = lazy(() => import('./pages/NoteEditor').then(m => ({ default: m.NoteEditor })));
const Todos = lazy(() => import('./pages/Todos').then(m => ({ default: m.Todos })));
const TodoEditor = lazy(() => import('./pages/TodoEditor').then(m => ({ default: m.TodoEditor })));
const Search = lazy(() => import('./pages/Search').then(m => ({ default: m.Search })));
const Calendar = lazy(() => import('./pages/Calendar').then(m => ({ default: m.Calendar })));
const Analytics = lazy(() => import('./pages/Analytics').then(m => ({ default: m.Analytics })));
const Favorites = lazy(() => import('./pages/Favorites').then(m => ({ default: m.Favorites })));
const Templates = lazy(() => import('./pages/Templates').then(m => ({ default: m.Templates })));
const Archive = lazy(() => import('./pages/Archive').then(m => ({ default: m.Archive })));
const Trash = lazy(() => import('./pages/Trash').then(m => ({ default: m.Trash })));
const Settings = lazy(() => import('./pages/Settings').then(m => ({ default: m.Settings })));
const Vault = lazy(() => import('./pages/Vault').then(m => ({ default: m.Vault })));

const isAuthenticated = (): boolean => localStorage.getItem('token') !== null;

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) =>
  isAuthenticated()
    ? <div className="flex flex-1 flex-col min-h-0 w-full overflow-hidden">{children}</div>
    : <Navigate to="/login" replace />;

const LazyPage: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <Suspense fallback={<PageLoader />}>
    <div className="flex flex-1 flex-col min-h-0 w-full overflow-hidden">{children}</div>
  </Suspense>
);

const AppLayout: React.FC = () => {
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const isLoginPage = location.pathname === '/login';

  if (isLoginPage) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
      </Routes>
    );
  }

  return (
    <div className="flex h-dvh max-h-dvh bg-background overflow-hidden">
      <Sidebar isMobileMenuOpen={isMobileMenuOpen} setIsMobileMenuOpen={setIsMobileMenuOpen} />

      <div className="flex-1 flex flex-col overflow-hidden min-w-0">
        <div className="lg:hidden flex items-center justify-between px-4 py-2.5">
          <button
            onClick={() => setIsMobileMenuOpen(true)}
            className="p-2 text-text hover:bg-hover rounded-xl transition-colors duration-[180ms]"
            aria-label="Open menu"
          >
            <i className="fas fa-bars text-lg"></i>
          </button>
          <div className="w-8 h-8 rounded-[10px] bg-[#0B0E11] flex items-center justify-center text-[#EDEFF2]">
            <CogitorMark size={20} />
          </div>
          <button
            type="button"
            onClick={openCommandPalette}
            className="p-2 text-text hover:bg-hover rounded-xl transition-colors duration-[180ms]"
            aria-label="Open command palette"
          >
            <i className="fas fa-search"></i>
          </button>
        </div>
        <CommandPalette />

        <div className="flex flex-1 flex-col min-h-0 w-full overflow-hidden">
          <Routes>
            <Route path="/" element={<ProtectedRoute><LazyPage><Dashboard /></LazyPage></ProtectedRoute>} />
            <Route path="/notes" element={<ProtectedRoute><LazyPage><Notes /></LazyPage></ProtectedRoute>} />
            <Route path="/notes/:id" element={<ProtectedRoute><LazyPage><NoteEditor /></LazyPage></ProtectedRoute>} />
            <Route path="/todos" element={<ProtectedRoute><LazyPage><Todos /></LazyPage></ProtectedRoute>} />
            <Route path="/todos/:id" element={<ProtectedRoute><LazyPage><TodoEditor /></LazyPage></ProtectedRoute>} />
            <Route path="/search" element={<ProtectedRoute><LazyPage><Search /></LazyPage></ProtectedRoute>} />
            <Route path="/calendar" element={<ProtectedRoute><LazyPage><Calendar /></LazyPage></ProtectedRoute>} />
            <Route path="/analytics" element={<ProtectedRoute><LazyPage><Analytics /></LazyPage></ProtectedRoute>} />
            <Route path="/favorites" element={<ProtectedRoute><LazyPage><Favorites /></LazyPage></ProtectedRoute>} />
            <Route path="/templates" element={<ProtectedRoute><LazyPage><Templates /></LazyPage></ProtectedRoute>} />
            <Route path="/archive" element={<ProtectedRoute><LazyPage><Archive /></LazyPage></ProtectedRoute>} />
            <Route path="/trash" element={<ProtectedRoute><LazyPage><Trash /></LazyPage></ProtectedRoute>} />
            <Route path="/vault" element={<ProtectedRoute><LazyPage><Vault /></LazyPage></ProtectedRoute>} />
            <Route path="/settings" element={<ProtectedRoute><LazyPage><Settings /></LazyPage></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </div>
    </div>
  );
};

const App: React.FC = () => (
  <ThemeProvider>
    <ToastProvider>
      <HashRouter>
        <AppLayout />
      </HashRouter>
    </ToastProvider>
  </ThemeProvider>
);

export default App;
