import React, { useState } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './contexts/ThemeContext';
import { ShortcutProvider } from './contexts/ShortcutContext';
import { Sidebar } from './components/Sidebar';
import { Dashboard } from './pages/Dashboard';
import { Notes } from './pages/Notes';
import { NoteEditor } from './pages/NoteEditor';
import { Todos } from './pages/Todos';
import { TodoEditor } from './pages/TodoEditor';
import { Search } from './pages/Search';
import { Calendar } from './pages/Calendar';
import { Analytics } from './pages/Analytics';
import { Favorites } from './pages/Favorites';
import { Templates } from './pages/Templates';
import { Archive } from './pages/Archive';
import { Trash } from './pages/Trash';
import { Settings } from './pages/Settings';
import Login from './pages/Login';

// Check if user is authenticated
const isAuthenticated = (): boolean => {
  return localStorage.getItem('token') !== null;
};

// Protected route wrapper
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return isAuthenticated() ? <>{children}</> : <Navigate to="/login" replace />;
};

const App: React.FC = () => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <ThemeProvider>
      <HashRouter>
        <ShortcutProvider>
          <div className="flex h-screen bg-background overflow-hidden">
            <Sidebar isMobileMenuOpen={isMobileMenuOpen} setIsMobileMenuOpen={setIsMobileMenuOpen} />

            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="lg:hidden flex items-center justify-between p-4 bg-surface border-b border-border">
                <button
                  onClick={() => setIsMobileMenuOpen(true)}
                  className="p-2 text-text hover:bg-hover rounded-lg transition-colors"
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
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
                <Route path="/notes" element={<ProtectedRoute><Notes /></ProtectedRoute>} />
                <Route path="/notes/:id" element={<ProtectedRoute><NoteEditor /></ProtectedRoute>} />
                <Route path="/todos" element={<ProtectedRoute><Todos /></ProtectedRoute>} />
                <Route path="/todos/:id" element={<ProtectedRoute><TodoEditor /></ProtectedRoute>} />
                <Route path="/search" element={<ProtectedRoute><Search /></ProtectedRoute>} />
                <Route path="/calendar" element={<ProtectedRoute><Calendar /></ProtectedRoute>} />
                <Route path="/analytics" element={<ProtectedRoute><Analytics /></ProtectedRoute>} />
                <Route path="/favorites" element={<ProtectedRoute><Favorites /></ProtectedRoute>} />
                <Route path="/templates" element={<ProtectedRoute><Templates /></ProtectedRoute>} />
                <Route path="/archive" element={<ProtectedRoute><Archive /></ProtectedRoute>} />
                <Route path="/trash" element={<ProtectedRoute><Trash /></ProtectedRoute>} />
                <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
              </Routes>
              </div>
            </div>
          </div>
        </ShortcutProvider>
      </HashRouter>
    </ThemeProvider>
  );
};

export default App;
