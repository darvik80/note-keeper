import React, { useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import mermaid from 'mermaid';

interface MarkdownRendererProps {
  content: string;
}

export const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content }) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    mermaid.initialize({
      startOnLoad: true,
      theme: 'default',
      securityLevel: 'loose'
    });

    if (containerRef.current) {
      const mermaidBlocks = containerRef.current.querySelectorAll('.language-mermaid');
      mermaidBlocks.forEach((block, index) => {
        const code = block.textContent || '';
        const id = `mermaid-${Date.now()}-${index}`;
        const div = document.createElement('div');
        div.id = id;
        div.className = 'mermaid-diagram';
        block.parentElement?.replaceChild(div, block);
        
        mermaid.render(id, code).then(({ svg }) => {
          div.innerHTML = svg;
        }).catch(err => {
          div.innerHTML = `<pre class="text-red-500">Mermaid Error: ${err.message}</pre>`;
        });
      });
    }
  }, [content]);

  return (
    <div ref={containerRef} className="markdown-content prose max-w-none">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code({ node, inline, className, children, ...props }) {
            const match = /language-(\w+)/.exec(className || '');
            const lang = match ? match[1] : '';
            
            if (!inline && lang === 'mermaid') {
              return (
                <pre className={className}>
                  <code className={className} {...props}>
                    {children}
                  </code>
                </pre>
              );
            }
            
            return !inline ? (
              <pre className="bg-gray-100 p-4 rounded-lg overflow-x-auto">
                <code className={className} {...props}>
                  {children}
                </code>
              </pre>
            ) : (
              <code className="bg-gray-100 px-2 py-1 rounded text-sm" {...props}>
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
            <blockquote className="border-l-4 border-primary pl-4 italic my-4 text-gray-700">
              {children}
            </blockquote>
          ),
          table: ({ children }) => (
            <div className="overflow-x-auto mb-4">
              <table className="min-w-full border-collapse border border-gray-300">
                {children}
              </table>
            </div>
          ),
          th: ({ children }) => (
            <th className="border border-gray-300 px-4 py-2 bg-gray-100 font-bold">
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td className="border border-gray-300 px-4 py-2">
              {children}
            </td>
          )
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};
