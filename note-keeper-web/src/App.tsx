import React, { useState } from 'react';
import { HashRouter, Routes, Route } from 'react-router-dom';
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

              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/notes" element={<Notes />} />
                <Route path="/notes/:id" element={<NoteEditor />} />
                <Route path="/todos" element={<Todos />} />
                <Route path="/todos/:id" element={<TodoEditor />} />
                <Route path="/search" element={<Search />} />
                <Route path="/calendar" element={<Calendar />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/favorites" element={<Favorites />} />
                <Route path="/templates" element={<Templates />} />
                <Route path="/archive" element={<Archive />} />
                <Route path="/trash" element={<Trash />} />
                <Route path="/settings" element={<Settings />} />
              </Routes>
            </div>
          </div>
        </ShortcutProvider>
      </HashRouter>
    </ThemeProvider>
  );
};

export default App;
