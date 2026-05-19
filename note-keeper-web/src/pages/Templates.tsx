/**
 * @module Templates
 * @category Pages
 * @description Note templates page — browse, create, and apply templates.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { NoteTemplate, NoteInput, NoteTemplateInput } from '../types';

/** Templates page for managing reusable note templates. */
export const Templates: React.FC = () => {
  const navigate = useNavigate();
  const [templates, setTemplates] = useState<NoteTemplate[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [newTemplate, setNewTemplate] = useState({
    name: '',
    content: '',
    tags: [] as string[],
    category: 'Work'
  });

  useEffect(() => {
    const load = async () => {
      try {
        const t = await api.templates.getAll();
        setTemplates(t);
      } catch (err) {
        setError((err as any)?.message || 'Failed to load templates');
      }
    };
    load();
  }, []);

  const createFromTemplate = async (template: NoteTemplate) => {
    const input: NoteInput = {
      title: `New from ${template.name}`,
      content: template.content,
      tags: [...template.tags],
      folder: 'default',
      priority: 'medium',
      isFavorite: false,
      isEncrypted: false,
      templateId: template.id,
    };
    try {
      const newNote = await api.notes.create(input);
      navigate(`/notes/${newNote.id}`);
    } catch (err) {
      setError((err as any)?.message || 'Failed to create note from template');
    }
  };

  const saveNewTemplate = async () => {
    if (!newTemplate.name.trim()) return;
    const input: NoteTemplateInput = {
      name: newTemplate.name,
      content: newTemplate.content,
      tags: newTemplate.tags,
      category: newTemplate.category,
    };
    try {
      const template = await api.templates.create(input);
      setTemplates(prev => [template, ...prev]);
      setShowCreate(false);
      setNewTemplate({ name: '', content: '', tags: [], category: 'Work' });
    } catch (err) {
      setError((err as any)?.message || 'Failed to create template');
    }
  };

  const deleteTemplate = async (id: string) => {
    if (confirm('Delete this template?')) {
      try {
        await api.templates.delete(id);
        setTemplates(prev => prev.filter(t => t.id !== id));
      } catch (err) {
        setError((err as any)?.message || 'Failed to delete template');
      }
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-gray-50">
      {error && (
        <div className="flex items-center gap-3 px-4 py-3 bg-red-50 border-b border-red-200 text-red-700 text-sm">
          <i className="fas fa-circle-exclamation shrink-0"></i>
          <span className="flex-1">{error}</span>
          <button onClick={() => setError(null)} className="shrink-0 hover:text-red-900">
            <i className="fas fa-times"></i>
          </button>
        </div>
      )}
      <Header
        title="Templates"
        actions={
          <button
            onClick={() => setShowCreate(!showCreate)}
            className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
          >
            <i className="fas fa-plus mr-2"></i>
            New Template
          </button>
        }
      />

      <div className="flex-1 overflow-auto p-4 lg:p-8">
        {showCreate && (
          <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 mb-6">
            <h3 className="text-lg font-bold mb-4">Create New Template</h3>
            <div className="space-y-4">
              <input
                type="text"
                placeholder="Template name"
                value={newTemplate.name}
                onChange={(e) => setNewTemplate({ ...newTemplate, name: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              />
              <select
                value={newTemplate.category}
                onChange={(e) => setNewTemplate({ ...newTemplate, category: e.target.value })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              >
                <option value="Work">Work</option>
                <option value="Personal">Personal</option>
                <option value="Study">Study</option>
                <option value="Other">Other</option>
              </select>
              <textarea
                placeholder="Template content (Markdown supported)"
                value={newTemplate.content}
                onChange={(e) => setNewTemplate({ ...newTemplate, content: e.target.value })}
                className="w-full h-48 px-4 py-2 border border-gray-300 rounded-lg font-mono text-sm"
              />
              <div className="flex gap-3">
                <button
                  onClick={saveNewTemplate}
                  className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
                >
                  Save Template
                </button>
                <button
                  onClick={() => setShowCreate(false)}
                  className="px-6 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
          {templates.map(template => (
            <div
              key={template.id}
              className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <h4 className="font-bold text-dark text-lg mb-1">{template.name}</h4>
                  <span className="text-xs bg-primary/10 text-primary px-2 py-1 rounded">
                    {template.category}
                  </span>
                </div>
                <button
                  onClick={() => deleteTemplate(template.id)}
                  className="text-red-500 hover:text-red-700 p-1"
                >
                  <i className="fas fa-trash"></i>
                </button>
              </div>
              
              <pre className="text-sm text-gray-600 mb-4 line-clamp-4 whitespace-pre-wrap font-mono bg-gray-50 p-3 rounded">
                {template.content}
              </pre>
              
              <div className="flex items-center justify-between">
                <div className="flex flex-wrap gap-2">
                  {template.tags.slice(0, 3).map(tag => (
                    <span key={tag} className="text-xs bg-gray-100 px-2 py-1 rounded">
                      #{tag}
                    </span>
                  ))}
                </div>
                <button
                  onClick={() => createFromTemplate(template)}
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm"
                >
                  <i className="fas fa-file-circle-plus mr-2"></i>
                  Use
                </button>
              </div>
            </div>
          ))}
        </div>

        {templates.length === 0 && !showCreate && (
          <div className="text-center py-16">
            <i className="fas fa-file-lines text-6xl text-gray-300 mb-4"></i>
            <p className="text-gray-500 text-lg">No templates yet</p>
          </div>
        )}
      </div>
    </div>
  );
};
