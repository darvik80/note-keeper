import React from 'react';

export const ApiTab: React.FC = () => (
  <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
    <div className="mb-6">
      <h3 className="text-2xl font-bold text-text mb-2">REST API Documentation</h3>
      <p className="text-text-secondary">
        Complete OpenAPI 3.0 specification for NoteKeeper REST API
      </p>
    </div>

    <div className="bg-background rounded-lg p-6 mb-6">
      <h4 className="text-lg font-bold text-text mb-4">API Endpoints</h4>
      <div className="space-y-4">
        <div className="border-l-4 border-primary pl-4">
          <h5 className="font-bold text-text mb-2">Notes API</h5>
          <div className="space-y-2 text-sm">
            {[
              ['GET', 'bg-green-500/15 text-green-700', '/api/v1/notes', 'Get all notes'],
              ['POST', 'bg-blue-500/15 text-blue-700', '/api/v1/notes', 'Create note'],
              ['GET', 'bg-green-500/15 text-green-700', '/api/v1/notes/:id', 'Get note by ID'],
              ['PUT', 'bg-yellow-500/15 text-yellow-700', '/api/v1/notes/:id', 'Update note'],
              ['DELETE', 'bg-red-500/15 text-red-600', '/api/v1/notes/:id', 'Delete note'],
              ['POST', 'bg-blue-500/15 text-blue-700', '/api/v1/notes/import', 'Import from file'],
            ].map(([method, cls, path, desc]) => (
              <div key={path + method} className="flex items-center gap-3 flex-wrap">
                <span className={`px-2 py-1 rounded font-mono text-xs ${cls}`}>{method}</span>
                <code className="text-text-secondary">{path}</code>
                <span className="text-text-secondary">- {desc}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="border-l-4 border-secondary pl-4">
          <h5 className="font-bold text-text mb-2">Todos API</h5>
          <div className="space-y-2 text-sm">
            {[
              ['GET', '/api/v1/todos', 'Get all todos'],
              ['POST', '/api/v1/todos', 'Create todo'],
              ['GET', '/api/v1/todos/:id', 'Get todo by ID'],
              ['PUT', '/api/v1/todos/:id', 'Update todo'],
              ['DELETE', '/api/v1/todos/:id', 'Delete todo'],
            ].map(([method, path, desc]) => (
              <div key={path + method} className="flex items-center gap-3 flex-wrap">
                <span className="px-2 py-1 bg-green-500/15 text-green-700 rounded font-mono text-xs">{method}</span>
                <code className="text-text-secondary">{path}</code>
                <span className="text-text-secondary">- {desc}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>

    <div className="bg-background rounded-lg p-6">
      <h4 className="text-lg font-bold text-text mb-4">Full Swagger Specification</h4>
      <p className="text-text-secondary mb-4">
        The complete OpenAPI 3.0 specification is available in the project root:
      </p>
      <div className="bg-surface border border-border rounded-lg p-4 flex items-center justify-between gap-4 flex-wrap">
        <code className="text-primary font-mono text-sm">swagger.yaml</code>
        <a
          href="/swagger.yaml"
          download="swagger.yaml"
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors text-sm"
        >
          <i className="fas fa-download mr-2"></i>
          Download
        </a>
      </div>
    </div>
  </div>
);
