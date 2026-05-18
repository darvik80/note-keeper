/**
 * @module NoteEditor
 * @category Pages
 * @description Note editor page — edit title, Markdown content, tags, folder, reminder, and attachments.
 */
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { MarkdownRenderer } from '../components/MarkdownRenderer';
import { ShareModal } from '../components/ShareModal';
import { Note, Attachment, NoteInput } from '../types';

/** Note editor page. Loads an existing note by route param `id`, supports Markdown preview, tag management, attachments, history restore, and sharing. */
export const NoteEditor: React.FC = () => {
  const params = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [note, setNote] = useState<Note | null>(null);
  const noteRef = React.useRef<Note | null>(null);
  const [isPreview, setIsPreview] = useState(true);
  const [showHistory, setShowHistory] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [tagInput, setTagInput] = useState('');

  // Keep ref in sync with state so saveNote/addTag always see latest note
  noteRef.current = note;

  useEffect(() => {
    if (!params.id) return;
    let cancelled = false;
    const load = async () => {
      try {
        const n = await api.notes.getById(params.id);
        if (cancelled) return;
        if (!n.content || n.content.trim() === '') {
          setIsPreview(false);
        }
        setNote(n);
      } catch (error) {
        console.error('Error loading note:', error);
        navigate('/notes');
      }
    };
    load();
    return () => { cancelled = true; };
  }, [params.id]);

  // ESC to exit fullscreen
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isFullscreen) {
        setIsFullscreen(false);
      }
    };
    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, [isFullscreen]);

  const saveNote = async () => {
    const current = noteRef.current;
    if (!current) return;

    try {
      // Upload new attachments (those with blob: URLs) to server
      const uploadedAttachments: any[] = [];
      if (current.attachments && current.attachments.length > 0) {
        for (const att of current.attachments) {
          if (att.url.startsWith('blob:')) {
            // This is a local file, need to upload
            // Fetch the blob from the blob URL
            const response = await fetch(att.url);
            const blob = await response.blob();
            const file = new File([blob], att.name, { type: att.type });
            // Upload to server
            const uploaded = await api.attachments.upload(file, current.id, 'note');
            uploadedAttachments.push(uploaded);
          } else {
            // Already uploaded to server
            uploadedAttachments.push(att);
          }
        }
      }

      const input: NoteInput = {
        title: current.title,
        content: current.content,
        tags: current.tags ?? [],
        folder: current.folder,
        subfolder: current.subfolder,
        priority: current.priority,
        isFavorite: current.isFavorite,
        isEncrypted: current.isEncrypted,
        // Convert local date to ISO UTC format for backend
        reminder: current.reminder ? (typeof current.reminder === 'string' ? current.reminder : current.reminder.toISOString()) : undefined,
        templateId: current.templateId,
        // Include uploaded attachments
        attachments: uploadedAttachments.map(att => ({
          id: att.id,
          name: att.name,
          size: att.size,
          type: att.type,
          url: att.url,
          uploadedAt: att.uploadedAt instanceof Date ? att.uploadedAt.toISOString() : att.uploadedAt
        })),
      };
      await api.notes.update(current.id, input);
      navigate('/notes');
    } catch (error) {
      console.error('Error saving note:', error);
      alert('Failed to save note. Please try again.');
    }
  };

  const addTag = (tag: string) => {
    if (!noteRef.current || !tag.trim()) return;
    const trimmed = tag.trim();
    setNote(prev => {
      if (!prev) return prev;
      const currentTags = prev.tags ?? [];
      if (currentTags.includes(trimmed)) return prev;
      return { ...prev, tags: [...currentTags, trimmed] };
    });
    setTagInput('');
  };

  const removeTag = (tag: string) => {
    setNote(prev => {
      if (!prev) return prev;
      return { ...prev, tags: (prev.tags ?? []).filter(t => t !== tag) };
    });
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || !noteRef.current) return;

    const newAttachments: Attachment[] = Array.from(files).map(file => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: URL.createObjectURL(file),
      uploadedAt: new Date()
    }));

    setNote(prev => prev ? { ...prev, attachments: [...(prev.attachments || []), ...newAttachments] } : prev);
  };

  const removeAttachment = (id: string) => {
    setNote(prev => prev ? { ...prev, attachments: (prev.attachments || []).filter(a => a.id !== id) } : prev);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (!note) return <div>Loading...</div>;

  return (
    <div className="flex-1 flex flex-col bg-white">
      <div className="border-b border-gray-200 px-4 sm:px-8 py-3 sm:py-4 flex items-center justify-between gap-2">
        <button
          onClick={() => navigate('/notes')}
          className="text-gray-600 hover:text-gray-800 shrink-0 flex items-center gap-1"
        >
          <i className="fas fa-arrow-left"></i>
          <span className="hidden sm:inline ml-1">Back</span>
        </button>
        <div className="flex items-center gap-1 sm:gap-3 overflow-x-auto">
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="p-2 hover:bg-gray-100 rounded-lg shrink-0"
            title="History"
          >
            <i className={`fas fa-clock-rotate-left ${showHistory ? 'text-primary' : 'text-gray-400'}`}></i>
          </button>
          <button
            onClick={() => setIsPreview(!isPreview)}
            className={`p-2 sm:px-4 sm:py-2 rounded-lg transition-colors shrink-0 ${
              isPreview ? 'bg-primary text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
            title={isPreview ? 'Edit' : 'Preview'}
          >
            <i className={`fas ${isPreview ? 'fa-edit' : 'fa-eye'} sm:mr-2`}></i>
            <span className="hidden sm:inline">{isPreview ? 'Edit' : 'Preview'}</span>
          </button>
          <button
            onClick={() => setNote(prev => prev ? { ...prev, isFavorite: !prev.isFavorite } : prev)}
            className="p-2 hover:bg-gray-100 rounded-lg shrink-0"
            title="Favorite"
          >
            <i className={`fas fa-star ${note.isFavorite ? 'text-yellow-500' : 'text-gray-400'}`}></i>
          </button>
          <button
            onClick={() => setNote(prev => prev ? { ...prev, isEncrypted: !prev.isEncrypted } : prev)}
            className="p-2 hover:bg-gray-100 rounded-lg shrink-0"
            title="Encrypt"
          >
            <i className={`fas fa-lock ${note.isEncrypted ? 'text-purple-500' : 'text-gray-400'}`}></i>
          </button>
          <button
            onClick={() => setShowShareModal(true)}
            className="p-2 hover:bg-gray-100 rounded-lg shrink-0"
            title="Share"
          >
            <i className="fas fa-share-alt text-gray-400"></i>
          </button>
          <select
            value={note.priority}
            onChange={(e) => { const v = e.target.value as any; setNote(prev => prev ? { ...prev, priority: v } : prev); }}
            className="px-2 py-2 border border-gray-300 rounded-lg text-sm shrink-0"
          >
            <option value="low">Low</option>
            <option value="medium">Med</option>
            <option value="high">High</option>
          </select>
          <button
            onClick={saveNote}
            className="p-2 sm:px-6 sm:py-2 bg-primary text-white rounded-lg hover:bg-primary/90 shrink-0"
            title="Save"
          >
            <i className="fas fa-save sm:mr-2"></i>
            <span className="hidden sm:inline">Save</span>
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-4 sm:p-8">
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
                        const c = entry.content;
                        setNote(prev => prev ? { ...prev, content: c } : prev);
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
          onChange={(e) => { const v = e.target.value; setNote(prev => prev ? { ...prev, title: v } : prev); }}
          className="text-2xl sm:text-4xl font-bold mb-4 sm:mb-6 w-full border-none outline-none"
          placeholder="Note Title"
        />

        {isPreview ? (
          <div className={`border border-gray-300 rounded-lg p-6 bg-white ${isFullscreen ? 'fixed inset-4 z-50 max-h-none' : 'min-h-96 max-h-[calc(100vh-400px)]'} overflow-y-auto`}>
            <div className="flex justify-end mb-2">
              <button
                onClick={() => setIsFullscreen(!isFullscreen)}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                title={isFullscreen ? 'Exit fullscreen' : 'Fullscreen'}
              >
                <i className={`fas ${isFullscreen ? 'fa-compress' : 'fa-expand'}`}></i>
              </button>
            </div>
            <MarkdownRenderer content={note.content} />
          </div>
        ) : (
          <textarea
            value={note.content}
            onChange={(e) => { const v = e.target.value; setNote(prev => prev ? { ...prev, content: v } : prev); }}
            className="w-full h-96 p-4 border border-gray-300 rounded-lg resize-none focus:outline-none focus:border-primary font-mono"
            placeholder="Start writing your note... (Markdown & Mermaid supported)"
          />
        )}

        <div className="mt-6 space-y-4">
          <div className="flex flex-wrap gap-2">
            {(note.tags ?? []).map(tag => (
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
            <input
              type="text"
              placeholder="Add tag + Enter"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              className="px-3 py-1 border border-gray-300 rounded-lg text-sm w-40"
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  addTag(tagInput);
                }
              }}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <input
              type="text"
              value={note.folder}
              onChange={(e) => { const v = e.target.value; setNote(prev => prev ? { ...prev, folder: v } : prev); }}
              className="px-4 py-2 border border-gray-300 rounded-lg w-full"
              placeholder="Folder"
            />
            <input
              type="text"
              value={note.subfolder || ''}
              onChange={(e) => { const v = e.target.value || undefined; setNote(prev => prev ? { ...prev, subfolder: v } : prev); }}
              className="px-4 py-2 border border-gray-300 rounded-lg w-full"
              placeholder="Subfolder (optional)"
            />
            <input
              type="datetime-local"
              value={note.reminder ? new Date(note.reminder).toISOString().slice(0, 16) : ''}
              onChange={(e) => { const v = e.target.value ? new Date(e.target.value) : undefined; setNote(prev => prev ? { ...prev, reminder: v } : prev); }}
              className="px-4 py-2 border border-gray-300 rounded-lg w-full"
            />
          </div>
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

      <ShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
        resourceId={note.id}
        resourceType="note"
        ownerId={note.ownerId}
        sharedWith={note.sharedWith}
        onShareSuccess={() => {
          // Reload note to get updated sharedWith
          if (params.id) {
            api.notes.getById(params.id).then(setNote);
          }
        }}
      />
    </div>
  );
};
