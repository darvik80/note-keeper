import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownPreviewProps {
  content: string;
  maxLines?: 3 | 4;
  className?: string;
  emptyText?: string;
}

/** Lightweight markdown preview for list cards — no Mermaid, compact styling. */
export const MarkdownPreview: React.FC<MarkdownPreviewProps> = ({
  content,
  maxLines = 3,
  className = '',
  emptyText = 'No content',
}) => {
  if (!content?.trim()) {
    return <span className="text-text-secondary italic text-sm">{emptyText}</span>;
  }

  const sanitized = content
    .replace(/```mermaid[\s\S]*?```/gi, '_diagram_')
    .replace(/```[\s\S]*?```/g, (block) => {
      const firstLine = block.split('\n')[1]?.trim();
      return firstLine ? `\`${firstLine.slice(0, 40)}${firstLine.length > 40 ? '…' : ''}\`` : '_code_';
    });

  const lineClamp = maxLines === 4 ? 'line-clamp-4' : 'line-clamp-3';

  return (
    <div className={`markdown-preview ${lineClamp} ${className}`}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          h1: ({ children }) => <strong className="block font-bold">{children}</strong>,
          h2: ({ children }) => <strong className="block font-bold">{children}</strong>,
          h3: ({ children }) => <strong className="block font-semibold">{children}</strong>,
          h4: ({ children }) => <strong className="block font-semibold">{children}</strong>,
          h5: ({ children }) => <strong className="block">{children}</strong>,
          h6: ({ children }) => <strong className="block">{children}</strong>,
          p: ({ children }) => <p>{children}</p>,
          ul: ({ children }) => <ul>{children}</ul>,
          ol: ({ children }) => <ol>{children}</ol>,
          li: ({ children }) => <li>{children}</li>,
          blockquote: ({ children }) => <blockquote>{children}</blockquote>,
          code: ({ children }) => <code>{children}</code>,
          pre: ({ children }) => <span className="preview-code-block">{children}</span>,
          a: ({ children }) => <span className="text-primary underline-offset-2">{children}</span>,
          img: ({ alt }) => <span className="preview-placeholder">[{alt || 'image'}]</span>,
          table: () => <span className="preview-placeholder">[table]</span>,
          hr: () => <span className="preview-placeholder"> — </span>,
          input: ({ checked }) => (
            <span className="mr-1">{checked ? '☑' : '☐'}</span>
          ),
        }}
      >
        {sanitized}
      </ReactMarkdown>
    </div>
  );
};
