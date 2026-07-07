import React, { Suspense, lazy, useState } from 'react';
import { HashRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { ThemeProvider } from './contexts/ThemeContext';
import { ToastProvider } from './contexts/ToastContext';
import { ShortcutProvider } from './contexts/ShortcutContext';
import { Sidebar } from './components/Sidebar';
import { PageLoader } from './components/PageLoader';
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

const isAuthenticated = (): boolean => localStorage.getItem('token') !== null;

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) =>
  isAuthenticated() ? <>{children}</> : <Navigate to="/login" replace />;

const LazyPage: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <Suspense fallback={<PageLoader />}>{children}</Suspense>
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
    <div className="flex h-screen bg-background overflow-hidden">
      <Sidebar isMobileMenuOpen={isMobileMenuOpen} setIsMobileMenuOpen={setIsMobileMenuOpen} />

      <div className="flex-1 flex flex-col overflow-hidden min-w-0">
        <div className="lg:hidden flex items-center justify-between p-4 bg-surface border-b border-border">
          <button
            onClick={() => setIsMobileMenuOpen(true)}
            className="p-2 text-text hover:bg-hover rounded-lg transition-colors"
            aria-label="Open menu"
          >
            <i className="fas fa-bars text-xl"></i>
          </button>
          <h1 className="text-lg font-bold text-primary flex items-center gap-2">
            <i className="fas fa-book"></i>
            NoteKeeper
          </h1>
          <div className="w-10"></div>
        </div>

        <div className="flex-1 flex flex-col min-h-0 overflow-hidden">
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
        <ShortcutProvider>
          <AppLayout />
        </ShortcutProvider>
      </HashRouter>
    </ToastProvider>
  </ThemeProvider>
);

export default App;
