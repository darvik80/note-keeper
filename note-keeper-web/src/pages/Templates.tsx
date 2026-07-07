/**
 * @module Templates
 * @category Pages
 * @description Note templates page — browse, create, and apply templates.
 */
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { PageShell } from '../components/PageShell';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { EmptyState } from '../components/EmptyState';
import { MarkdownPreview } from '../components/MarkdownPreview';
import { useToast } from '../contexts/ToastContext';
import { api } from '../utils/api';
import { NoteInput, NoteTemplate, NoteTemplateInput } from '../types';

export const Templates: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [templates, setTemplates] = useState<NoteTemplate[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [newTemplate, setNewTemplate] = useState({
    name: '',
    content: '',
    tags: [] as string[],
    category: 'Work',
  });

  useEffect(() => {
    const load = async () => {
      try {
        setTemplates(await api.templates.getAll());
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
      toast.success('Template created');
    } catch (err) {
      setError((err as any)?.message || 'Failed to create template');
    }
  };

  const confirmDeleteTemplate = async () => {
    if (!deleteId) return;
    setDeleteLoading(true);
    try {
      await api.templates.delete(deleteId);
      setTemplates(prev => prev.filter(t => t.id !== deleteId));
      toast.success('Template deleted');
    } catch (err) {
      setError((err as any)?.message || 'Failed to delete template');
    } finally {
      setDeleteLoading(false);
      setDeleteId(null);
    }
  };

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
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

      <div className="flex-1 min-h-0 w-full overflow-y-auto overflow-x-hidden">
        <div className="p-4 lg:p-8">
          {showCreate && (
            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border mb-6">
              <h3 className="text-lg font-bold text-text mb-4">Create New Template</h3>
              <div className="space-y-4">
                <input
                  type="text"
                  placeholder="Template name"
                  value={newTemplate.name}
                  onChange={(e) => setNewTemplate({ ...newTemplate, name: e.target.value })}
                  className="input-field"
                />
                <select
                  value={newTemplate.category}
                  onChange={(e) => setNewTemplate({ ...newTemplate, category: e.target.value })}
                  className="input-field"
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
                  className="input-field h-48 font-mono text-sm resize-y"
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
                    className="px-6 py-2 btn-secondary rounded-lg"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}

          {templates.length === 0 && !showCreate ? (
            <EmptyState icon="fa-file-lines" message="No templates yet" />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
              {templates.map(template => (
                <div
                  key={template.id}
                  className="bg-surface rounded-xl p-6 shadow-sm border border-border hover:shadow-md transition-shadow flex flex-col"
                >
                  <div className="flex items-start justify-between mb-3 gap-2">
                    <div className="flex-1 min-w-0">
                      <h4 className="font-bold text-text text-lg mb-1 truncate">{template.name}</h4>
                      <span className="text-xs bg-primary/10 text-primary px-2 py-1 rounded">
                        {template.category}
                      </span>
                    </div>
                    <button
                      onClick={() => setDeleteId(template.id)}
                      className="text-red-500 hover:text-red-600 p-1 shrink-0"
                      aria-label="Delete template"
                    >
                      <i className="fas fa-trash"></i>
                    </button>
                  </div>

                  <div className="bg-background rounded-lg p-3 mb-4 flex-1 min-h-0">
                    <MarkdownPreview
                      content={template.content}
                      maxLines={4}
                      emptyText="Empty template"
                    />
                  </div>

                  <div className="flex items-center justify-between gap-2 mt-auto">
                    <div className="flex flex-wrap gap-2">
                      {template.tags.slice(0, 3).map(tag => (
                        <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                    <button
                      onClick={() => createFromTemplate(template)}
                      className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 text-sm shrink-0"
                    >
                      <i className="fas fa-file-circle-plus mr-2"></i>
                      Use
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <ConfirmDialog
        isOpen={deleteId !== null}
        onClose={() => setDeleteId(null)}
        onConfirm={confirmDeleteTemplate}
        title="Delete template"
        message="Are you sure you want to delete this template? This action cannot be undone."
        confirmLabel="Delete"
        variant="danger"
        loading={deleteLoading}
      />
    </PageShell>
  );
};
