/**
 * @module NoteEditor
 * @category Pages
 * @description Note editor page — edit title, Markdown content, tags, folder, reminder, and attachments.
 */
import React, {useCallback, useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {api} from '../utils/api';
import {PageShell} from '../components/PageShell';
import {ConfirmDialog} from '../components/ConfirmDialog';
import {useToast} from '../contexts/ToastContext';
import {MarkdownRenderer} from '../components/MarkdownRenderer';
import {MarkdownEditor, useHydrateAttachmentImages} from '../components/MarkdownEditor';
import {ShareModal} from '../components/ShareModal';
import {TagInput} from '../components/TagInput';
import {useAutosave} from '../hooks/useAutosave';
import {Attachment, Note, NoteHistory, NoteInput} from '../types';

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
  const [error, setError] = useState<string | null>(null);
  const [restoreEntry, setRestoreEntry] = useState<NoteHistory | null>(null);
  const [dirty, setDirty] = useState(false);
  const [saveFailed, setSaveFailed] = useState(false);
  const [leaveOpen, setLeaveOpen] = useState(false);
  const { toast } = useToast();
  const previewRef = useHydrateAttachmentImages(note?.content ?? '');

  // Keep ref in sync with state so saveNote/addTag always see latest note
  noteRef.current = note;

  useEffect(() => {
    if (!showHistory || !note?.id) return;
    let cancelled = false;
    api.notes.getHistory(note.id).then((history) => {
      if (!cancelled) setNote(prev => prev ? { ...prev, history } : prev);
    }).catch(() => { /* keep nested history if fetch fails */ });
    return () => { cancelled = true; };
  }, [showHistory, note?.id]);

  useEffect(() => {
    const id = params.id;
    if (!id) return;
    let cancelled = false;
    const load = async () => {
      try {
        const n = await api.notes.getById(id);
        if (cancelled) return;
        if (!n.content || n.content.trim() === '') {
          setIsPreview(false);
        }
        setNote(n);
        setDirty(false);
      } catch (error: any) {
        if (!cancelled) setError(error?.message || 'Failed to load note');
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

  const saveNote = useCallback(async (leave = false) => {
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
      setDirty(false);
      setSaveFailed(false);
      if (leave) navigate('/notes');
    } catch (error: any) {
      setSaveFailed(true);
      setError(error?.message || 'Failed to save note');
      throw error;
    }
  }, [navigate]);

  const autosaveStatus = useAutosave(
    dirty,
    () => saveNote(false),
    800,
    `${note?.title ?? ''}\n${note?.content ?? ''}\n${(note?.tags ?? []).join(',')}`
  );

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        saveNote(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [saveNote]);



  const formatDateTimeLocal = (date: Date | string): string => {
    const d = typeof date === 'string' ? new Date(date) : new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
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

  if (!note) return (
    <div className="flex-1 flex flex-col items-center justify-center gap-4 p-8">
      {error ? (
        <div className="flex flex-col items-center gap-3 text-center">
          <i className="fas fa-circle-exclamation text-red-400 text-4xl"></i>
          <p className="text-red-600 font-medium">{error}</p>
          <button
            onClick={() => navigate('/notes')}
            className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
          >
            Back to Notes
          </button>
        </div>
      ) : (
        <div className="text-text-secondary flex items-center gap-2">
          <i className="fas fa-spinner fa-spin"></i>
          Loading...
        </div>
      )}
    </div>
  );

  return (
    <PageShell error={error} onDismissError={() => setError(null)} className="overflow-hidden">
      <div className="border-b border-border px-4 sm:px-8 py-3 sm:py-4 flex items-center justify-between gap-2">
        <button
          onClick={() => {
            if (saveFailed && dirty) setLeaveOpen(true);
            else navigate('/notes');
          }}
          className="text-text-secondary hover:text-text shrink-0 flex items-center gap-1"
        >
          <i className="fas fa-arrow-left"></i>
          <span className="hidden sm:inline ml-1">Back</span>
        </button>
        <div className="flex items-center gap-1 sm:gap-3 overflow-x-auto">
          <button
            onClick={() => setShowHistory(!showHistory)}
            className={`p-2 sm:px-4 sm:py-2 rounded-lg transition-colors shrink-0 ${
              showHistory ? 'bg-primary text-white' : 'bg-hover text-text hover:bg-hover/80'
            }`}
            title="History"
          >
            <i className={`fas fa-clock-rotate-left sm:mr-2 ${showHistory ? '' : 'text-text-secondary'}`}></i>
            <span className="hidden sm:inline">History</span>
          </button>
          <button
            onClick={() => setIsPreview(!isPreview)}
            className={`p-2 sm:px-4 sm:py-2 rounded-lg transition-colors shrink-0 ${
              isPreview ? 'bg-primary text-white' : 'bg-hover text-text hover:bg-hover/80'
            }`}
            title={isPreview ? 'Edit' : 'Preview'}
          >
            <i className={`fas ${isPreview ? 'fa-edit' : 'fa-eye'} sm:mr-2`}></i>
            <span className="hidden sm:inline">{isPreview ? 'Edit' : 'Preview'}</span>
          </button>
          <button
            onClick={() => setNote(prev => prev ? { ...prev, isFavorite: !prev.isFavorite } : prev)}
            className="p-2 hover:bg-hover rounded-lg shrink-0"
            title="Favorite"
          >
            <i className={`fas fa-star ${note.isFavorite ? 'text-yellow-500' : 'text-text-secondary'}`}></i>
          </button>
          <button
            onClick={() => setNote(prev => prev ? { ...prev, isEncrypted: !prev.isEncrypted } : prev)}
            className="p-2 hover:bg-hover rounded-lg shrink-0"
            title="Encrypt"
          >
            <i className={`fas fa-lock ${note.isEncrypted ? 'text-purple-500' : 'text-text-secondary'}`}></i>
          </button>
          <button
            onClick={() => setShowShareModal(true)}
            className="p-2 hover:bg-hover rounded-lg shrink-0"
            title="Share"
          >
            <i className="fas fa-share-alt text-text-secondary"></i>
          </button>
          <select
            value={note.priority}
            onChange={(e) => { const v = e.target.value as any; setNote(prev => prev ? { ...prev, priority: v } : prev); }}
            className="px-2 py-2 border border-border rounded-lg text-sm shrink-0 bg-surface text-text"
          >
            <option value="low">Low</option>
            <option value="medium">Med</option>
            <option value="high">High</option>
          </select>
          <span className="text-xs text-text-secondary shrink-0">
            {autosaveStatus === 'saving' ? 'Saving…' : autosaveStatus === 'saved' && !dirty ? 'Saved' : dirty ? 'Unsaved' : ''}
          </span>
          <button
            onClick={() => saveNote(false)}
            className="p-2 sm:px-6 sm:py-2 bg-primary text-white rounded-lg hover:bg-primary/90 shrink-0"
            title="Save"
          >
            <i className="fas fa-save sm:mr-2"></i>
            <span className="hidden sm:inline">Save</span>
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-4 sm:p-8 min-h-0">
        {showHistory && (
          <div className="mb-6 p-4 bg-surface border border-border rounded-lg">
            <h3 className="text-sm font-bold text-text mb-3 flex items-center gap-2">
              <i className="fas fa-clock-rotate-left"></i>
              History {note.history?.length ? `(${note.history.length} versions)` : ''}
            </h3>
            {note.history && note.history.length > 0 ? (
              <div className="space-y-2 max-h-48 overflow-y-auto">
                {[...note.history].reverse().map((entry) => (
                  <div key={entry.id} className="flex items-center justify-between gap-2 p-2 bg-hover rounded border border-border">
                    <div className="flex items-center gap-3 min-w-0">
                      <span className="text-xs font-mono text-text-secondary shrink-0">
                        {new Date(entry.timestamp).toLocaleString()}
                      </span>
                      <span className="text-xs px-2 py-1 bg-hover text-text rounded">
                        {entry.action}
                      </span>
                    </div>
                    <button
                      onClick={() => setRestoreEntry(entry)}
                      className="text-xs px-3 py-1 text-primary hover:bg-hover rounded shrink-0"
                    >
                      <i className="fas fa-rotate-left mr-1"></i>
                      Restore
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-text-secondary">No versions yet. Save the note to start history.</p>
            )}
          </div>
        )}

        <input
          type="text"
          value={note.title}
          onChange={(e) => { const v = e.target.value; setNote(prev => prev ? { ...prev, title: v } : prev); setDirty(true); }}
          className="text-2xl sm:text-4xl font-bold mb-4 sm:mb-6 w-full border-none outline-none bg-transparent text-text"
          placeholder="Note Title"
        />

        <div className={`${isFullscreen ? 'fixed inset-4 z-50 bg-background p-4 rounded-xl' : ''} grid gap-4 ${!isPreview ? 'lg:grid-cols-2' : 'grid-cols-1'}`}>
          {!isPreview && (
            <MarkdownEditor
              value={note.content}
              onChange={(v) => { setNote(prev => prev ? { ...prev, content: v } : prev); setDirty(true); }}
              placeholder="Write…"
              minHeight="24rem"
              onImageUpload={async (file) => {
                if (!note.id) throw new Error('Save note first, then upload images');
                const uploaded = await api.attachments.upload(file, note.id, 'note');
                if (!uploaded?.id) throw new Error('Upload failed');
                api.attachments.rememberPreview(uploaded.id, file);
                const url = api.attachments.markdownUrl(uploaded.id);
                const attachment: Attachment = {
                  id: uploaded.id,
                  name: file.name || uploaded.name,
                  size: uploaded.size || file.size,
                  type: uploaded.type || file.type,
                  url,
                  uploadedAt: uploaded.uploadedAt ? new Date(uploaded.uploadedAt) : new Date(),
                };
                setNote(prev => prev ? { ...prev, attachments: [...(prev.attachments || []), attachment] } : prev);
                setDirty(true);
                return url;
              }}
            />
          )}
          <div ref={previewRef} data-nk-preview className={`border border-border rounded-xl p-6 bg-surface text-text ${isFullscreen ? 'max-h-none' : 'min-h-96 max-h-[calc(100vh-400px)]'} overflow-y-auto ${!isPreview ? 'hidden lg:block' : ''}`}>
              <div className="flex justify-end mb-2">
                <button
                  onClick={() => setIsFullscreen(!isFullscreen)}
                  className="p-2 hover:bg-hover rounded-lg transition-colors text-text-secondary"
                  title={isFullscreen ? 'Exit fullscreen' : 'Fullscreen'}
                >
                  <i className={`fas ${isFullscreen ? 'fa-compress' : 'fa-expand'}`}></i>
                </button>
              </div>
              {note.content?.trim()
                ? <MarkdownRenderer content={note.content} />
                : <p className="text-text-secondary text-sm">Preview appears here as you write.</p>}
            </div>
        </div>
        {!isPreview && (
          <button
            type="button"
            className="lg:hidden mt-3 px-4 py-2 border border-border rounded-lg"
            onClick={() => setIsPreview(true)}
          >
            Preview
          </button>
        )}

        <div className="mt-6 space-y-4">
          <TagInput
            tags={note.tags ?? []}
            onChange={(newTags) => { setNote(prev => prev ? { ...prev, tags: newTags } : prev); setDirty(true); }}
          />

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <input
              type="text"
              value={note.folder}
              onChange={(e) => { const v = e.target.value; setNote(prev => prev ? { ...prev, folder: v } : prev); }}
              className="px-4 py-2 border border-border rounded-lg w-full bg-surface text-text"
              placeholder="Folder"
            />
            <input
              type="text"
              value={note.subfolder || ''}
              onChange={(e) => { const v = e.target.value || undefined; setNote(prev => prev ? { ...prev, subfolder: v } : prev); }}
              className="px-4 py-2 border border-border rounded-lg w-full bg-surface text-text"
              placeholder="Subfolder (optional)"
            />
            <input
              type="datetime-local"
              value={note.reminder ? formatDateTimeLocal(note.reminder) : ''}
              onChange={(e) => { const v = e.target.value ? new Date(e.target.value) : undefined; setNote(prev => prev ? { ...prev, reminder: v } : prev); }}
              className="px-4 py-2 border border-border rounded-lg w-full bg-surface text-text"
            />
          </div>
        </div>

        <div className="mt-6">
          <label className="block text-sm font-medium text-text-secondary mb-2">
            <i className="fas fa-paperclip mr-2"></i>
            Attachments
          </label>
          <div className="flex flex-wrap gap-2">
            {note.attachments?.map(attachment => (
              <span
                key={attachment.id}
                className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-hover border border-border text-sm text-text"
                title={formatFileSize(attachment.size)}
              >
                <i className={`fas ${attachment.type?.startsWith('image/') ? 'fa-image' : 'fa-paperclip'} text-text-secondary`}></i>
                <span className="max-w-[10rem] truncate">{attachment.name}</span>
                <a
                  href={attachment.url}
                  download={attachment.name}
                  className="text-primary hover:opacity-80"
                  title="Download"
                >
                  <i className="fas fa-download"></i>
                </a>
                <button
                  type="button"
                  onClick={() => removeAttachment(attachment.id)}
                  className="text-text-secondary hover:text-red-400"
                  title="Remove"
                >
                  <i className="fas fa-xmark"></i>
                </button>
              </span>
            ))}
            <label className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-dashed border-border text-sm text-text-secondary hover:border-primary hover:text-primary cursor-pointer">
              <i className="fas fa-plus"></i>
              File
              <input
                type="file"
                multiple
                onChange={handleFileUpload}
                className="hidden"
              />
            </label>
          </div>
        </div>

        <div className="mt-6 p-4 bg-hover rounded-lg">
          <h4 className="text-sm font-bold text-text mb-2">Markdown Cheatsheet:</h4>
          <div className="text-xs text-text-secondary space-y-1">
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
          const id = params.id;
          if (id) {
            api.notes.getById(id).then(setNote);
          }
        }}
      />
      <ConfirmDialog
        isOpen={restoreEntry !== null}
        onClose={() => setRestoreEntry(null)}
        onConfirm={() => {
          const entry = restoreEntry;
          if (!entry || !note) return;
          setRestoreEntry(null);
          void (async () => {
            try {
              const updated = await api.notes.restoreVersion(note.id, entry.id);
              setNote(updated);
              setDirty(false);
              setIsPreview(true);
              setShowHistory(true);
              toast.success('Version restored');
            } catch (err: any) {
              setNote(prev => prev ? { ...prev, content: entry.content } : prev);
              setDirty(true);
              setIsPreview(true);
              toast.error(err?.message || 'Restored locally — save failed');
            }
          })();
        }}
        title="Restore version"
        message="Replace current content with this historical version? This saves immediately and adds a new history row."
        confirmLabel="Restore"
      />
      <ConfirmDialog
        isOpen={leaveOpen}
        onClose={() => setLeaveOpen(false)}
        onConfirm={() => navigate('/notes')}
        title="Unsaved changes"
        message="Autosave failed. Leave anyway?"
        confirmLabel="Leave"
        variant="danger"
      />
    </PageShell>
  );
};
