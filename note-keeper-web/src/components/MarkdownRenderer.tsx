/**
 * @module MarkdownRenderer
 * @category Components
 * Renders Markdown with GFM and Mermaid. Mermaid SVGs live in a dedicated
 * host node so mermaid never `replaceChild`s React's tree (that throws
 * `NotFoundError: removeChild` on the next render).
 */
import React, { useEffect, useId, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import mermaid from 'mermaid';
import { useTheme } from '../contexts/ThemeContext';
import { isDarkTheme } from '../utils/themes';

interface MarkdownRendererProps {
  content: string;
}

function ensureMermaid(dark: boolean) {
  mermaid.initialize({
    startOnLoad: false,
    theme: dark ? 'dark' : 'neutral',
    securityLevel: 'loose',
  });
}

const MermaidBlock: React.FC<{ chart: string; dark: boolean }> = ({ chart, dark }) => {
  const hostRef = useRef<HTMLDivElement>(null);
  const reactId = useId().replace(/:/g, '');

  useEffect(() => {
    const host = hostRef.current;
    if (!host) return;
    const source = chart.trim();
    if (!source) {
      host.replaceChildren();
      return;
    }

    let cancelled = false;
    const renderId = `mermaid-${reactId}-${Math.random().toString(36).slice(2, 9)}`;
    ensureMermaid(dark);

    mermaid
      .render(renderId, source)
      .then(({ svg }) => {
        if (cancelled || !hostRef.current) return;
        hostRef.current.innerHTML = svg;
      })
      .catch(err => {
        if (cancelled || !hostRef.current) return;
        const msg = err instanceof Error ? err.message : String(err);
        hostRef.current.innerHTML = `<pre class="text-red-500 text-sm overflow-x-auto">Mermaid: ${msg}</pre>`;
      });

    return () => {
      cancelled = true;
      document.getElementById(renderId)?.remove();
      document.getElementById(`d${renderId}`)?.remove();
    };
  }, [chart, dark, reactId]);

  return <div ref={hostRef} className="mermaid-diagram" />;
};

export const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content }) => {
  const { theme } = useTheme();
  const dark = isDarkTheme(theme);

  return (
    <div className="markdown-content prose max-w-none">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          pre({ children }) {
            const child = React.Children.toArray(children)[0];
            if (React.isValidElement(child)) {
              const className = String(
                (child.props as { className?: string }).className || ''
              );
              const lang = /language-(\w+)/.exec(className)?.[1];
              if (lang === 'mermaid') {
                const raw = (child.props as { children?: React.ReactNode }).children;
                return <MermaidBlock chart={String(raw ?? '')} dark={dark} />;
              }
            }
            return (
              <pre className="bg-hover p-4 rounded-lg overflow-x-auto">{children}</pre>
            );
          },
          code({ className, children, ...props }) {
            const lang = /language-(\w+)/.exec(className || '')?.[1];
            if (lang === 'mermaid') {
              return <code className={className} {...props}>{children}</code>;
            }
            if (className) {
              return (
                <code className={className} {...props}>
                  {children}
                </code>
              );
            }
            return (
              <code className="bg-hover px-2 py-1 rounded text-sm" {...props}>
                {children}
              </code>
            );
          },
          h1: ({ children }) => <h1 className="text-3xl font-bold mb-4 mt-6">{children}</h1>,
          h2: ({ children }) => <h2 className="text-2xl font-bold mb-3 mt-5">{children}</h2>,
          h3: ({ children }) => <h3 className="text-xl font-bold mb-2 mt-4">{children}</h3>,
          p: ({ children }) => <p className="mb-4 leading-relaxed">{children}</p>,
          ul: ({ children }) => <ul className="list-disc list-inside mb-4 space-y-2">{children}</ul>,
          ol: ({ children }) => <ol className="list-decimal list-inside mb-4 space-y-2">{children}</ol>,
          blockquote: ({ children }) => (
            <blockquote className="border-l-4 border-primary pl-4 italic my-4 text-text-secondary">
              {children}
            </blockquote>
          ),
          table: ({ children }) => (
            <div className="overflow-x-auto mb-4">
              <table className="min-w-full border-collapse border border-border">
                {children}
              </table>
            </div>
          ),
          th: ({ children }) => (
            <th className="border border-border px-4 py-2 bg-hover font-bold">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="border border-border px-4 py-2">
              {children}
            </td>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};
