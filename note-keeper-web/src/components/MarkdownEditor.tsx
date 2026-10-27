import React, { useCallback, useMemo, useRef } from 'react';
import CodeMirror, { type ReactCodeMirrorRef } from '@uiw/react-codemirror';
import { markdown } from '@codemirror/lang-markdown';
import { EditorView } from '@codemirror/view';

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  minHeight?: string;
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

export const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  value,
  onChange,
  placeholder = 'Write markdown…',
  minHeight = '16rem',
  onImagePaste,
}) => {
  const editorRef = useRef<ReactCodeMirrorRef>(null);

  const apply = useCallback((transform: (selected: string) => string) => {
    const view = editorRef.current?.view;
    if (!view) {
      onChange(transform(value));
      return;
    }
    const { from, to } = view.state.selection.main;
    const selected = view.state.sliceDoc(from, to);
    const insert = transform(selected);
    view.dispatch({
      changes: { from, to, insert },
      selection: { anchor: from + insert.length },
    });
  }, [onChange, value]);

  const extensions = useMemo(() => [
    markdown(),
    EditorView.lineWrapping,
    EditorView.domEventHandlers({
      paste: (event, view) => {
        const items = event.clipboardData?.items;
        if (!items || !onImagePaste) return false;
        for (const item of Array.from(items)) {
          if (!item.type.startsWith('image/')) continue;
          const file = item.getAsFile();
          if (!file) continue;
          event.preventDefault();
          void onImagePaste(file).then(url => {
            if (!url) return;
            const insert = `![${file.name}](${url})`;
            view.dispatch(view.state.replaceSelection(insert));
          });
          return true;
        }
        return false;
      }
    })
  ], [onImagePaste]);

  return (
    <div className="border border-border rounded-lg overflow-hidden bg-surface">
      <div className="flex gap-1 p-2 border-b border-border bg-hover/40">
        <ToolbarButton icon="fa-bold" title="Bold" onClick={() => apply(s => wrap(s || 'bold', '**'))} />
        <ToolbarButton icon="fa-list-ul" title="List" onClick={() => apply(s => prefixLines(s, '- ', '- item\n'))} />
        <ToolbarButton icon="fa-square-check" title="Task" onClick={() => apply(s => prefixLines(s, '- [ ] ', '- [ ] task\n'))} />
        <ToolbarButton icon="fa-code" title="Code" onClick={() => apply(s => wrap(s || 'code', '`'))} />
        <ToolbarButton icon="fa-link" title="Link" onClick={() => apply(s => `[${s || 'text'}](url)`)} />
        <ToolbarButton icon="fa-image" title="Image" onClick={() => apply(s => `![${s || 'alt'}](url)`)} />
      </div>
      <CodeMirror
        ref={editorRef}
        value={value}
        height={minHeight}
        placeholder={placeholder}
        extensions={extensions}
        onChange={onChange}
        basicSetup={{ lineNumbers: false, foldGutter: false }}
      />
    </div>
  );
};

const ToolbarButton: React.FC<{ icon: string; title: string; onClick: () => void }> = ({ icon, title, onClick }) => (
  <button type="button" title={title} onClick={onClick} className="px-2 py-1 text-text-secondary hover:bg-hover rounded">
    <i className={`fas ${icon}`}></i>
  </button>
);
