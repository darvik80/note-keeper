import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { MarkdownRenderer } from '../components/MarkdownRenderer';
import { Note, Attachment, NoteInput } from '../types';

export const NoteEditor: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [note, setNote] = useState<Note | null>(null);
  const [isPreview, setIsPreview] = useState(true);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    if (id) {
      const load = async () => {
        try {
          const n = await api.notes.getById(id);
          setNote(n);
        } catch (err) {
          console.error('Failed to load note', err);
        }
      };
      load();
    }
  }, [id]);

  const saveNote = async () => {
    if (!note) return;
    const input: NoteInput = {
      title: note.title,
      content: note.content,
      tags: note.tags,
      folder: note.folder,
      subfolder: note.subfolder,
      priority: note.priority,
      isFavorite: note.isFavorite,
      isEncrypted: note.isEncrypted,
      reminder: note.reminder ? String(note.reminder) : undefined,
      templateId: note.templateId,
    };
    try {
      await api.notes.update(note.id, input);
      navigate('/notes');
    } catch (err) {
      console.error('Failed to save note', err);
    }
  };

  const addTag = (tag: string) => {
    if (!note || !tag.trim() || note.tags.includes(tag)) return;
    setNote({ ...note, tags: [...note.tags, tag.trim()] });
  };

  const removeTag = (tag: string) => {
    if (!note) return;
    setNote({ ...note, tags: note.tags.filter(t => t !== tag) });
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || !note) return;

    const newAttachments: Attachment[] = Array.from(files).map(file => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: URL.createObjectURL(file),
      uploadedAt: new Date()
    }));

    setNote({ ...note, attachments: [...(note.attachments || []), ...newAttachments] });
  };

  const removeAttachment = (id: string) => {
    if (!note) return;
    setNote({ ...note, attachments: (note.attachments || []).filter(a => a.id !== id) });
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (!note) return <div>Loading...</div>;

  return (
    <div className="flex-1 flex flex-col bg-white">
      <div className="border-b border-gray-200 px-8 py-4 flex items-center justify-between">
        <button
          onClick={() => navigate('/notes')}
          className="text-gray-600 hover:text-gray-800"
        >
          <i className="fas fa-arrow-left mr-2"></i>
          Back to Notes
        </button>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="p-2 hover:bg-gray-100 rounded-lg"
            title="History"
          >
            <i className={`fas fa-clock-rotate-left ${showHistory ? 'text-primary' : 'text-gray-400'}`}></i>
          </button>
          <button
            onClick={() => setIsPreview(!isPreview)}
            className={`px-4 py-2 rounded-lg transition-colors ${
              isPreview ? 'bg-primary text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <i className={`fas ${isPreview ? 'fa-edit' : 'fa-eye'} mr-2`}></i>
            {isPreview ? 'Edit' : 'Preview'}
          </button>
          <button
            onClick={() => setNote({ ...note, isFavorite: !note.isFavorite })}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            <i className={`fas fa-star ${note.isFavorite ? 'text-yellow-500' : 'text-gray-400'}`}></i>
          </button>
          <button
            onClick={() => setNote({ ...note, isEncrypted: !note.isEncrypted })}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            <i className={`fas fa-lock ${note.isEncrypted ? 'text-purple-500' : 'text-gray-400'}`}></i>
          </button>
          <select
            value={note.priority}
            onChange={(e) => setNote({ ...note, priority: e.target.value as any })}
            className="px-3 py-2 border border-gray-300 rounded-lg"
          >
            <option value="low">Low Priority</option>
            <option value="medium">Medium Priority</option>
            <option value="high">High Priority</option>
          </select>
          <button
            onClick={saveNote}
            className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
          >
            <i className="fas fa-save mr-2"></i>
            Save
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-8">
        {showHistory && note.history && note.history.length > 0 && (
          <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h3 className="text-sm font-bold text-blue-900 mb-3 flex items-center gap-2">
              <i className="fas fa-clock-rotate-left"></i>
              History ({note.history.length} versions)
            </h3>
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {[...note.history].reverse().map((entry, index) => (
                <div key={entry.id} className="flex items-center justify-between p-2 bg-white rounded border border-blue-100">
                  <div className="flex items-center gap-3">
                    <span className="text-xs font-mono text-blue-600">
                      {new Date(entry.timestamp).toLocaleString()}
                    </span>
                    <span className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded">
                      {entry.action}
                    </span>
                  </div>
                  <button
                    onClick={() => {
                      if (confirm('Restore this version?')) {
                        setNote({ ...note, content: entry.content });
                      }
                    }}
                    className="text-xs px-3 py-1 text-primary hover:bg-primary/10 rounded"
                  >
                    <i className="fas fa-rotate-left mr-1"></i>
                    Restore
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        <input
          type="text"
          value={note.title}
          onChange={(e) => setNote({ ...note, title: e.target.value })}
          className="text-4xl font-bold mb-6 w-full border-none outline-none"
          placeholder="Note Title"
        />

        <div className="mb-6">
          <div className="flex flex-wrap gap-2 mb-3">
            {note.tags.map(tag => (
              <span
                key={tag}
                className="bg-primary/10 text-primary px-3 py-1 rounded-full text-sm flex items-center gap-2"
              >
                #{tag}
                <button onClick={() => removeTag(tag)} className="hover:text-primary/70">
                  <i className="fas fa-times"></i>
                </button>
              </span>
            ))}
          </div>
          <input
            type="text"
            placeholder="Add tag and press Enter"
            className="px-4 py-2 border border-gray-300 rounded-lg w-64"
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                addTag((e.target as HTMLInputElement).value);
                (e.target as HTMLInputElement).value = '';
              }
            }}
          />
        </div>

        {isPreview ? (
          <div className="border border-gray-300 rounded-lg p-6 bg-white min-h-96">
            <MarkdownRenderer content={note.content} />
          </div>
        ) : (
          <textarea
            value={note.content}
            onChange={(e) => setNote({ ...note, content: e.target.value })}
            className="w-full h-96 p-4 border border-gray-300 rounded-lg resize-none focus:outline-none focus:border-primary font-mono"
            placeholder="Start writing your note... (Markdown & Mermaid supported)"
          />
        )}

        <div className="mt-6 flex items-center gap-4">
          <input
            type="text"
            value={note.folder}
            onChange={(e) => setNote({ ...note, folder: e.target.value })}
            className="px-4 py-2 border border-gray-300 rounded-lg"
            placeholder="Folder"
          />
          <input
            type="text"
            value={note.subfolder || ''}
            onChange={(e) => setNote({ ...note, subfolder: e.target.value || undefined })}
            className="px-4 py-2 border border-gray-300 rounded-lg"
            placeholder="Subfolder (optional)"
          />
          <input
            type="datetime-local"
            value={note.reminder ? new Date(note.reminder).toISOString().slice(0, 16) : ''}
            onChange={(e) => setNote({ ...note, reminder: e.target.value ? new Date(e.target.value) : undefined })}
            className="px-4 py-2 border border-gray-300 rounded-lg"
          />
        </div>

        <div className="mt-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <i className="fas fa-paperclip mr-2"></i>
            Attachments
          </label>
          <div className="space-y-3">
            {note.attachments?.map(attachment => (
              <div key={attachment.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
                <div className="flex items-center gap-3">
                  <i className="fas fa-file text-gray-400 text-xl"></i>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{attachment.name}</p>
                    <p className="text-xs text-gray-500">{formatFileSize(attachment.size)}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <a
                    href={attachment.url}
                    download={attachment.name}
                    className="p-2 text-primary hover:bg-primary/10 rounded-lg transition-colors"
                  >
                    <i className="fas fa-download"></i>
                  </a>
                  <button
                    onClick={() => removeAttachment(attachment.id)}
                    className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <i className="fas fa-trash"></i>
                  </button>
                </div>
              </div>
            ))}
            <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-primary hover:bg-primary/5 transition-colors cursor-pointer">
              <i className="fas fa-cloud-upload-alt text-gray-400"></i>
              <span className="text-sm text-gray-600">Upload files</span>
              <input
                type="file"
                multiple
                onChange={handleFileUpload}
                className="hidden"
              />
            </label>
          </div>
        </div>

        <div className="mt-6 p-4 bg-gray-50 rounded-lg">
          <h4 className="text-sm font-bold text-gray-700 mb-2">Markdown Cheatsheet:</h4>
          <div className="text-xs text-gray-600 space-y-1">
            <p><code># H1</code>, <code>## H2</code>, <code>### H3</code> - Headers</p>
            <p><code>**bold**</code>, <code>*italic*</code>, <code>`code`</code> - Text formatting</p>
            <p><code>- item</code> or <code>1. item</code> - Lists</p>
            <p><code>[link](url)</code>, <code>![image](url)</code> - Links & Images</p>
            <p><code>```mermaid ... ```</code> - Mermaid diagrams</p>
          </div>
        </div>
      </div>
    </div>
  );
};
