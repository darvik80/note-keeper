import React, { useEffect, useState } from 'react';
import { api } from '../../utils/api';
import { ApiToken, ApiTokenScope } from '../../types';

const SCOPES: { id: ApiTokenScope; label: string }[] = [
  { id: 'notes', label: 'Notes' },
  { id: 'todos', label: 'Todos' },
  { id: 'vault', label: 'Vault' },
  { id: 'mcp', label: 'MCP' },
];

export const ApiTokensTab: React.FC = () => {
  const [tokens, setTokens] = useState<ApiToken[]>([]);
  const [name, setName] = useState('');
  const [scopes, setScopes] = useState<ApiTokenScope[]>(['notes', 'todos', 'vault', 'mcp']);
  const [created, setCreated] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = async () => {
    try {
      setTokens(await api.tokens.list());
    } catch (err: any) {
      setError(err?.message || 'Failed to load tokens');
    }
  };

  useEffect(() => { load(); }, []);

  const toggleScope = (scope: ApiTokenScope) => {
    setScopes(prev => prev.includes(scope) ? prev.filter(s => s !== scope) : [...prev, scope]);
  };

  const create = async () => {
    if (!name.trim() || scopes.length === 0) return;
    setBusy(true);
    setError(null);
    try {
      const token = await api.tokens.create({ name: name.trim(), scopes });
      setCreated(token.token || null);
      setName('');
      await load();
    } catch (err: any) {
      setError(err?.message || 'Failed to create token');
    } finally {
      setBusy(false);
    }
  };

  const revoke = async (id: string) => {
    if (!window.confirm('Revoke this token? Clients using it will stop working.')) return;
    try {
      await api.tokens.revoke(id);
      await load();
    } catch (err: any) {
      setError(err?.message || 'Failed to revoke token');
    }
  };

  const copy = async (value: string) => {
    await navigator.clipboard.writeText(value);
  };

  return (
    <div className="space-y-6">
      {error && <div className="p-3 bg-red-50 text-red-700 rounded-lg text-sm">{error}</div>}
      {created && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg">
          <p className="text-sm font-medium text-amber-900 mb-2">Copy this token now. It will not be shown again.</p>
          <div className="flex gap-2">
            <code className="flex-1 break-all bg-white border border-amber-200 rounded px-3 py-2 text-sm">{created}</code>
            <button onClick={() => copy(created)} className="px-3 py-2 bg-primary text-white rounded-lg text-sm">Copy</button>
          </div>
        </div>
      )}

      <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
        <h3 className="text-xl font-bold text-text mb-4">New token</h3>
        <input
          value={name}
          onChange={e => setName(e.target.value)}
          placeholder="Name (FAR home, Cursor MCP, …)"
          className="w-full px-4 py-2 border border-border rounded-lg mb-4 bg-background"
        />
        <div className="flex flex-wrap gap-2 mb-4">
          {SCOPES.map(scope => (
            <button
              key={scope.id}
              type="button"
              onClick={() => toggleScope(scope.id)}
              className={`px-3 py-1.5 rounded-full text-sm border ${
                scopes.includes(scope.id) ? 'bg-primary text-white border-primary' : 'border-border text-text-secondary'
              }`}
            >
              {scope.label}
            </button>
          ))}
        </div>
        <button
          onClick={create}
          disabled={busy || !name.trim() || scopes.length === 0}
          className="px-4 py-2 bg-primary text-white rounded-lg disabled:opacity-50"
        >
          Create token
        </button>
      </div>

      <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
        <h3 className="text-xl font-bold text-text mb-4">Existing tokens</h3>
        {tokens.length === 0 && <p className="text-text-secondary text-sm">No tokens yet.</p>}
        <div className="space-y-3">
          {tokens.map(token => (
            <div key={token.id} className="flex items-center justify-between gap-4 p-3 border border-border rounded-lg">
              <div>
                <p className="font-medium text-text">{token.name}</p>
                <p className="text-xs text-text-secondary font-mono">{token.prefix} · {token.scopes.join(', ')}</p>
              </div>
              <button onClick={() => revoke(token.id)} className="text-sm text-red-600 hover:underline">Revoke</button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
