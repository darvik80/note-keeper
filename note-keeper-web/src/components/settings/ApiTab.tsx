import React from 'react';

export const ApiTab: React.FC = () => {
  const apiDocsJson = `${window.location.origin}/v3/api-docs`;
  const apiDocsYaml = `${window.location.origin}/v3/api-docs.yaml`;

  return (
    <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
      <div className="mb-6">
        <h3 className="text-2xl font-bold text-text mb-2">REST API Documentation</h3>
        <p className="text-text-secondary">
          Interactive OpenAPI 3.0 documentation powered by springdoc-openapi
        </p>
      </div>

      <div className="bg-background rounded-lg p-6 mb-6">
        <h4 className="text-lg font-bold text-text mb-4">Swagger UI</h4>
        <p className="text-text-secondary mb-4">
          Explore and test all API endpoints directly from the interactive Swagger UI.
          Authentication is supported via the <code className="bg-surface px-1 rounded">Authorize</code> button (JWT Bearer token).
        </p>
        <a
          href="/swagger-ui/index.html"
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors text-sm"
        >
          <i className="fas fa-external-link-alt mr-2"></i>
          Open Swagger UI
        </a>
      </div>

      <div className="bg-background rounded-lg p-6">
        <h4 className="text-lg font-bold text-text mb-4">OpenAPI Specification</h4>
        <p className="text-text-secondary mb-4">
          Machine-readable OpenAPI 3.0 specification is available in JSON and YAML formats:
        </p>
        <div className="space-y-3">
          <div className="bg-surface border border-border rounded-lg p-4 flex items-center justify-between gap-4 flex-wrap">
            <div className="flex items-center gap-3">
              <code className="text-primary font-mono text-sm">/v3/api-docs</code>
              <span className="text-text-secondary text-sm">(JSON)</span>
            </div>
            <a
              href={apiDocsJson}
              target="_blank"
              rel="noopener noreferrer"
              className="px-3 py-1.5 bg-secondary text-white rounded-lg hover:bg-secondary/90 transition-colors text-sm"
            >
              View JSON
            </a>
          </div>
          <div className="bg-surface border border-border rounded-lg p-4 flex items-center justify-between gap-4 flex-wrap">
            <div className="flex items-center gap-3">
              <code className="text-primary font-mono text-sm">/v3/api-docs.yaml</code>
              <span className="text-text-secondary text-sm">(YAML)</span>
            </div>
            <a
              href={apiDocsYaml}
              target="_blank"
              rel="noopener noreferrer"
              className="px-3 py-1.5 bg-secondary text-white rounded-lg hover:bg-secondary/90 transition-colors text-sm"
            >
              View YAML
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
