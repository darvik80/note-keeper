import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import CodeMirror, { type ReactCodeMirrorRef } from '@uiw/react-codemirror';
import { markdown } from '@codemirror/lang-markdown';
import { HighlightStyle, syntaxHighlighting } from '@codemirror/language';
import { EditorView } from '@codemirror/view';
import { tags as t } from '@lezer/highlight';
import { api } from '../utils/api';
import { useTheme } from '../contexts/ThemeContext';
import { isDarkTheme } from '../utils/themes';
import './MarkdownEditor.css';

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  minHeight?: string;
  onImageUpload?: (file: File) => Promise<string | void>;
  /** @deprecated use onImageUpload */
  onImagePaste?: (file: File) => Promise<string | void>;
}

const wrap = (value: string, before: string, after = before): string => `${before}${value}${after}`;

const prefixLines = (selected: string, prefix: string, empty: string): string => {
  if (!selected) return empty;
  return selected
    .split('\n')
    .map(line => (line.startsWith(prefix) ? line : `${prefix}${line}`))
    .join('\n');
};

const headingPrefix = (level: 1 | 2 | 3) => {
  const marks = '#'.repeat(level);
  return (selected: string) => {
    if (!selected) return `${marks} heading`;
    return selected
      .split('\n')
      .map(line => `${marks} ${line.replace(/^#{1,6}\s*/, '')}`)
      .join('\n');
  };
};

const nkHighlight = HighlightStyle.define([
  { tag: t.heading, color: 'var(--color-primary)', fontWeight: '700' },
  { tag: t.heading1, color: 'var(--color-primary)', fontWeight: '700' },
  { tag: t.heading2, color: 'var(--color-primary)', fontWeight: '600' },
  { tag: t.strong, fontWeight: '700', color: 'var(--color-text)' },
  { tag: t.emphasis, fontStyle: 'italic' },
  { tag: t.link, color: 'var(--color-secondary)' },
  { tag: t.url, color: 'var(--color-secondary)' },
  { tag: t.monospace, color: 'var(--color-secondary)' },
  { tag: t.processingInstruction, color: 'var(--color-text-secondary)' },
  { tag: t.meta, color: 'var(--color-text-secondary)' },
  { tag: t.comment, color: 'var(--color-text-secondary)', fontStyle: 'italic' },
  { tag: t.keyword, color: 'var(--color-primary)' },
  { tag: t.string, color: 'var(--color-secondary)' },
  { tag: t.atom, color: 'var(--color-primary)' },
  { tag: t.bool, color: 'var(--color-primary)' },
  { tag: t.labelName, color: 'var(--color-secondary)' },
]);

const nkEditorTheme = (dark: boolean) =>
  EditorView.theme({
    '&': {
      backgroundColor: 'var(--color-surface)',
      color: 'var(--color-text)',
      fontSize: '15px',
      colorScheme: dark ? 'dark' : 'light',
    },
    '.cm-scroller': {
      fontFamily: 'inherit',
      lineHeight: '1.65',
      backgroundColor: 'var(--color-surface)',
    },
    '.cm-content': {
      caretColor: 'var(--color-primary)',
      padding: '12px 16px',
      minHeight: 'inherit',
      color: 'var(--color-text)',
      backgroundColor: 'var(--color-surface)',
    },
    '.cm-line': {
      color: 'var(--color-text)',
    },
    '.cm-cursor, .cm-dropCursor': {
      borderLeftColor: 'var(--color-primary)',
    },
    '&.cm-focused': {
      outline: 'none',
    },
    '.cm-selectionBackground, &.cm-focused > .cm-scroller > .cm-selectionLayer .cm-selectionBackground, .cm-content ::selection': {
      backgroundColor: 'color-mix(in srgb, var(--color-primary) 28%, transparent) !important',
    },
    '.cm-activeLine': {
      backgroundColor: 'color-mix(in srgb, var(--color-hover) 80%, transparent)',
    },
    '.cm-gutters': {
      display: 'none',
      backgroundColor: 'var(--color-surface)',
      border: 'none',
    },
    '.cm-placeholder': {
      color: 'var(--color-text-secondary)',
      fontStyle: 'normal',
    },
    '.cm-tooltip': {
      backgroundColor: 'var(--color-surface)',
      color: 'var(--color-text)',
      border: '1px solid var(--color-border)',
    },
    '.cm-matchingBracket': {
      backgroundColor: 'color-mix(in srgb, var(--color-primary) 22%, transparent)',
    },
  }, { dark });

const MERMAID_SNIPPET = '```mermaid\nflowchart LR\n  A --> B\n```\n';

const isImageFile = (file: File) => file.type.startsWith('image/');

/**
 * react-markdown strips `blob:` / `data:` src. Leave canonical download URLs in
 * markdown, then swap rendered `<img>` nodes to authed blob: URLs.
 */
export const useHydrateAttachmentImages = (content: string) => {
  const nodeRef = useRef<HTMLElement | null>(null);

  const scan = useCallback((root: HTMLElement | null) => {
    if (!root) return;
    root.querySelectorAll('img').forEach(img => {
      const src = img.getAttribute('src') || img.src || '';
      if (src.startsWith('blob:')) return;
      const match = src.match(/\/api\/v1\/attachments\/([a-zA-Z0-9-]+)\/download/);
      if (!match) return;
      if (img.dataset.nkHydrated === match[1]) return;
      const cached = api.attachments.previewUrl(match[1]);
      if (cached) {
        img.dataset.nkHydrated = match[1];
        img.src = cached;
        return;
      }
      void api.attachments.download(match[1]).then(blob => {
        const url = api.attachments.rememberPreview(match[1], blob);
        img.dataset.nkHydrated = match[1];
        img.src = url;
      }).catch(() => { /* keep download URL */ });
    });
  }, []);

  const setRef = useCallback((node: HTMLDivElement | null) => {
    nodeRef.current = node;
    scan(node);
  }, [scan]);

  useEffect(() => {
    const node = nodeRef.current;
    if (!node) return;
    scan(node);
    const obs = new MutationObserver(() => scan(node));
    obs.observe(node, { childList: true, subtree: true });
    const t = window.setTimeout(() => scan(node), 80);
    return () => { obs.disconnect(); window.clearTimeout(t); };
  }, [content, scan]);

  return setRef;
};

export const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  value,
  onChange,
  placeholder = 'Write…',
  minHeight = '16rem',
  onImageUpload,
  onImagePaste,
}) => {
  const editorRef = useRef<ReactCodeMirrorRef>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const { theme } = useTheme();
  const dark = isDarkTheme(theme);
  const [dragOver, setDragOver] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const uploadImage = onImageUpload || onImagePaste;
  const uploadImageRef = useRef(uploadImage);
  uploadImageRef.current = uploadImage;
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const valueRef = useRef(value);
  valueRef.current = value;

  const apply = useCallback((transform: (selected: string) => string) => {
    const view = editorRef.current?.view;
    if (!view) {
      onChangeRef.current(transform(valueRef.current));
      return;
    }
    const { from, to } = view.state.selection.main;
    const selected = view.state.sliceDoc(from, to);
    const insert = transform(selected);
    view.dispatch({
      changes: { from, to, insert },
      selection: { anchor: from + insert.length },
    });
  }, []);

  const insertAtCursor = useCallback((insert: string) => {
    const view = editorRef.current?.view;
    if (!view) {
      const current = valueRef.current;
      onChangeRef.current(current ? `${current}\n${insert}` : insert);
      return;
    }
    view.dispatch(view.state.replaceSelection(insert));
  }, []);

  const handleImageFile = useCallback(async (file: File) => {
    if (!isImageFile(file)) {
      setUploadError('Pick an image file');
      return;
    }
    const uploader = uploadImageRef.current;
    if (!uploader) {
      setUploadError('Save note first, then upload images');
      return;
    }
    setUploading(true);
    setUploadError(null);
    try {
      const url = await uploader(file);
      if (!url) {
        setUploadError('Upload failed');
        return;
      }
      const alt = file.name.replace(/\.[^.]+$/, '') || 'image';
      insertAtCursor(`\n![${alt}](${url})\n`);
    } catch (err: any) {
      setUploadError(err?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  }, [insertAtCursor]);

  const extensions = useMemo(() => [
    markdown(),
    EditorView.lineWrapping,
    syntaxHighlighting(nkHighlight),
    nkEditorTheme(dark),
    EditorView.domEventHandlers({
      paste: (event) => {
        const items = event.clipboardData?.items;
        if (!items || !uploadImageRef.current) return false;
        for (const item of Array.from(items)) {
          if (!item.type.startsWith('image/')) continue;
          const file = item.getAsFile();
          if (!file) continue;
          event.preventDefault();
          void handleImageFile(file);
          return true;
        }
        return false;
      },
    }),
  ], [dark, handleImageFile]);

  const onDrop = (event: React.DragEvent) => {
    event.preventDefault();
    setDragOver(false);
    const files = Array.from(event.dataTransfer.files).filter(isImageFile);
    if (files[0]) void handleImageFile(files[0]);
  };

  return (
    <div
      className="nk-md-editor relative border border-border rounded-xl overflow-hidden bg-surface"
      onDragEnter={e => { e.preventDefault(); setDragOver(true); }}
      onDragOver={e => { e.preventDefault(); setDragOver(true); }}
      onDragLeave={e => { e.preventDefault(); setDragOver(false); }}
      onDrop={onDrop}
    >
      <div className="flex flex-wrap gap-0.5 p-2 border-b border-border bg-hover/40">
        <ToolbarButton icon="fa-bold" title="Bold" onClick={() => apply(s => wrap(s || 'bold', '**'))} />
        <ToolbarButton icon="fa-italic" title="Italic" onClick={() => apply(s => wrap(s || 'italic', '*'))} />
        <ToolbarLabel title="Heading 1" onClick={() => apply(headingPrefix(1))}>H1</ToolbarLabel>
        <ToolbarLabel title="Heading 2" onClick={() => apply(headingPrefix(2))}>H2</ToolbarLabel>
        <ToolbarLabel title="Heading 3" onClick={() => apply(headingPrefix(3))}>H3</ToolbarLabel>
        <ToolbarButton icon="fa-list-ul" title="Bullet list" onClick={() => apply(s => prefixLines(s, '- ', '- item\n'))} />
        <ToolbarButton icon="fa-list-ol" title="Numbered list" onClick={() => apply(s => prefixLines(s, '1. ', '1. item\n'))} />
        <ToolbarButton icon="fa-square-check" title="Task" onClick={() => apply(s => prefixLines(s, '- [ ] ', '- [ ] task\n'))} />
        <ToolbarButton icon="fa-quote-right" title="Quote" onClick={() => apply(s => prefixLines(s, '> ', '> quote\n'))} />
        <ToolbarButton icon="fa-code" title="Code" onClick={() => apply(s => s.includes('\n') ? wrap(s || 'code', '```\n', '\n```') : wrap(s || 'code', '`'))} />
        <ToolbarButton icon="fa-link" title="Link" onClick={() => apply(s => `[${s || 'text'}](url)`)} />
        <ToolbarButton icon="fa-diagram-project" title="Mermaid diagram" onClick={() => insertAtCursor(MERMAID_SNIPPET)} />
        <button
          type="button"
          title="Upload image"
          disabled={uploading}
          onClick={() => {
            if (!uploadImage) {
              setUploadError('Save note first, then upload images');
              return;
            }
            fileRef.current?.click();
          }}
          className="px-2 py-1 text-sm text-text-secondary hover:text-primary hover:bg-hover rounded inline-flex items-center gap-1.5 disabled:opacity-50"
        >
          <i className={`fas ${uploading ? 'fa-spinner fa-spin' : 'fa-image'}`}></i>
          <span>Image</span>
        </button>
        <input
          ref={fileRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={e => {
            const file = e.target.files?.[0];
            e.target.value = '';
            if (file) void handleImageFile(file);
          }}
        />
      </div>
      {uploadError && (
        <p className="px-3 py-1.5 text-xs text-red-400 border-b border-border bg-hover/30">{uploadError}</p>
      )}
      <CodeMirror
        ref={editorRef}
        value={value}
        height={minHeight}
        placeholder={placeholder}
        extensions={extensions}
        onChange={onChange}
        theme="none"
        basicSetup={{
          lineNumbers: false,
          foldGutter: false,
          highlightActiveLine: false,
          highlightActiveLineGutter: false,
          syntaxHighlighting: false,
        }}
      />
      {dragOver && <div className="nk-md-drop">Drop image to upload</div>}
    </div>
  );
};

const ToolbarButton: React.FC<{ icon: string; title: string; onClick: () => void }> = ({ icon, title, onClick }) => (
  <button type="button" title={title} onClick={onClick} className="px-2 py-1 text-text-secondary hover:text-primary hover:bg-hover rounded">
    <i className={`fas ${icon}`}></i>
  </button>
);

const ToolbarLabel: React.FC<{ title: string; onClick: () => void; children: React.ReactNode }> = ({ title, onClick, children }) => (
  <button type="button" title={title} onClick={onClick} className="px-2 py-1 text-xs font-semibold text-text-secondary hover:text-primary hover:bg-hover rounded">
    {children}
  </button>
);
