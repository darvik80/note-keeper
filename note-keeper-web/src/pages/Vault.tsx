import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Header } from '../components/Header';
import { PageShell } from '../components/PageShell';
import { api } from '../utils/api';
import { VaultFolder, VaultItem, VaultItemInput, VaultItemType } from '../types';

const emptyItem = (folderId?: string): VaultItemInput => ({
  title: '',
  type: 'password',
  folderId,
  username: '',
  host: '',
  url: '',
  tags: [],
  password: '',
  privateKey: '',
  secret: '',
});

const toInput = (item: VaultItem): VaultItemInput => ({
  title: item.title,
  type: item.type,
  folderId: item.folderId,
  username: item.username,
  host: item.host,
  url: item.url,
  tags: item.tags,
  password: item.password,
  privateKey: item.privateKey,
  secret: item.secret,
  extraJson: item.extraJson,
});

const sameFolder = (a?: string, b?: string) => (a || '') === (b || '');

const generatePassword = (): string => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*';
  const bytes = new Uint8Array(20);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, b => chars[b % chars.length]).join('');
};

const FolderSelect: React.FC<{
  value?: string;
  folders: VaultFolder[];
  onChange: (id?: string) => void;
  noneLabel?: string;
  className?: string;
  ariaLabel?: string;
}> = ({ value, folders, onChange, noneLabel = 'No folder', className, ariaLabel }) => (
  <select
    value={value || ''}
    onChange={e => onChange(e.target.value || undefined)}
    className={className}
    aria-label={ariaLabel || 'Folder'}
  >
    <option value="">{noneLabel}</option>
    {folders.map(folder => (
      <option key={folder.id} value={folder.id}>{folder.name}</option>
    ))}
  </select>
);

export const Vault: React.FC = () => {
  const [folders, setFolders] = useState<VaultFolder[]>([]);
  const [items, setItems] = useState<VaultItem[]>([]);
  const [folderId, setFolderId] = useState<string | undefined>();
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState<(VaultItemInput & { id?: string; revision?: number }) | null>(null);
  const [showSecret, setShowSecret] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [newFolder, setNewFolder] = useState('');
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [dropTarget, setDropTarget] = useState<string | null>(null);
  const loadGen = useRef(0);

  const load = async (selected?: string, q?: string) => {
    const gen = ++loadGen.current;
    try {
      const [nextFolders, nextItems] = await Promise.all([
        api.vault.listFolders(),
        api.vault.listItems({ folder: selected, q: q || undefined })
      ]);
      if (gen !== loadGen.current) return;
      setFolders(nextFolders);
      setItems(nextItems);
    } catch (err: any) {
      if (gen !== loadGen.current) return;
      setError(err?.message || 'Failed to load vault');
    }
  };

  useEffect(() => { load(folderId, query); }, [folderId]);

  const search = async () => { await load(folderId, query); };

  const selectedFolder = useMemo(
    () => folders.find(f => f.id === folderId),
    [folders, folderId]
  );

  const folderName = (id?: string) => folders.find(f => f.id === id)?.name;

  const saveFolder = async () => {
    if (!newFolder.trim()) return;
    await api.vault.createFolder({ name: newFolder.trim(), parentId: folderId });
    setNewFolder('');
    await load(folderId, query);
  };

  const saveItem = async () => {
    if (!editing?.title.trim()) return;
    try {
      const body: VaultItemInput = {
        title: editing.title.trim(),
        type: editing.type,
        folderId: editing.folderId,
        username: editing.username,
        host: editing.host,
        url: editing.url,
        tags: editing.tags,
        password: editing.password,
        privateKey: editing.privateKey,
        secret: editing.secret,
        extraJson: editing.extraJson,
      };
      if (editing.id) {
        await api.vault.updateItem(editing.id, body, editing.revision);
      } else {
        await api.vault.createItem(body);
      }
      setEditing(null);
      setShowSecret(false);
      await load(folderId, query);
    } catch (err: any) {
      setError(err?.message || 'Failed to save item');
    }
  };

  const moveItem = async (item: VaultItem, nextFolderId?: string) => {
    if (sameFolder(item.folderId, nextFolderId)) return;
    try {
      await api.vault.updateItem(item.id, { ...toInput(item), folderId: nextFolderId }, item.revision);
      await load(folderId, query);
    } catch (err: any) {
      setError(err?.message || 'Failed to move item');
    }
  };

  const removeItem = async (id: string) => {
    if (!window.confirm('Delete this vault item?')) return;
    await api.vault.deleteItem(id);
    await load(folderId, query);
  };

  const copy = async (value?: string) => {
    if (value) await navigator.clipboard.writeText(value);
  };

  const downloadPem = (item: VaultItem) => {
    if (!item.privateKey) return;
    const blob = new Blob([item.privateKey], { type: 'application/x-pem-file' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${item.title || 'key'}.pem`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const chipClass = (on: boolean) =>
    `shrink-0 px-3 py-1.5 rounded-full text-[13px] whitespace-nowrap ${
      on ? 'bg-primary/15 text-primary' : 'text-text-secondary border border-border'
    }`;

  const folderBtnClass = (id: string | undefined, selected: boolean) => {
    const dropping = draggingId != null && dropTarget === (id ?? '');
    return `block w-full text-left px-3 py-2 rounded-xl mb-0.5 text-[14px] ${
      dropping
        ? 'bg-primary/25 text-primary ring-1 ring-primary/60'
        : selected
          ? 'bg-primary/15 text-primary'
          : 'text-text-secondary hover:bg-hover hover:text-text'
    }`;
  };

  const acceptDrop = (e: React.DragEvent, targetId?: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDropTarget(targetId ?? '');
  };

  const dropOnFolder = async (e: React.DragEvent, targetId?: string) => {
    e.preventDefault();
    const id = e.dataTransfer.getData('text/plain');
    const item = items.find(i => i.id === id);
    setDropTarget(null);
    setDraggingId(null);
    if (item) await moveItem(item, targetId);
  };

  return (
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header
        title="Vault"
        subtitle={selectedFolder ? selectedFolder.name : 'Secrets stay on your account'}
        actions={
          <button
            onClick={() => { setEditing(emptyItem(folderId)); setShowSecret(false); }}
            className="nk-btn nk-btn-primary hidden sm:inline-flex"
          >
            {selectedFolder ? `New in ${selectedFolder.name}` : 'New item'}
          </button>
        }
      />
      <div className="flex flex-1 min-h-0">
        <aside className="w-56 shrink-0 p-4 overflow-y-auto hidden sm:block">
          <p className="px-3 mb-1 text-[11px] font-medium uppercase tracking-wider text-text-secondary">Folders</p>
          <button
            onClick={() => setFolderId(undefined)}
            onDragOver={e => acceptDrop(e, '')}
            onDragLeave={() => setDropTarget(null)}
            onDrop={e => dropOnFolder(e, undefined)}
            className={folderBtnClass(undefined, !folderId)}
          >
            All items
          </button>
          {folders.map(folder => (
            <button
              key={folder.id}
              onClick={() => setFolderId(folder.id)}
              onDragOver={e => acceptDrop(e, folder.id)}
              onDragLeave={() => setDropTarget(null)}
              onDrop={e => dropOnFolder(e, folder.id)}
              className={folderBtnClass(folder.id, folderId === folder.id)}
            >
              {folder.name}
            </button>
          ))}
          <div className="mt-4 flex gap-2">
            <input
              value={newFolder}
              onChange={e => setNewFolder(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && saveFolder()}
              placeholder="New folder"
              className="flex-1 input-field py-1.5 text-sm"
            />
            <button onClick={saveFolder} className="nk-btn nk-btn-primary px-3">+</button>
          </div>
          <p className="px-3 mt-3 text-[12px] text-text-secondary leading-snug">
            Drop a secret on a folder to move it. Or pick a folder on the item.
          </p>
        </aside>

        <div className="flex-1 overflow-y-auto px-4 sm:px-6 pb-24 sm:pb-8">
          <div className="sm:hidden flex gap-2 overflow-x-auto pb-3 -mx-4 px-4">
            <button
              type="button"
              onClick={() => setFolderId(undefined)}
              className={chipClass(!folderId)}
            >
              All items
            </button>
            {folders.map(folder => (
              <button
                key={folder.id}
                type="button"
                onClick={() => setFolderId(folder.id)}
                className={chipClass(folderId === folder.id)}
              >
                {folder.name}
              </button>
            ))}
            <div className="flex gap-1 shrink-0">
              <input
                value={newFolder}
                onChange={e => setNewFolder(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && saveFolder()}
                placeholder="New folder"
                className="w-28 input-field py-1.5 text-sm"
              />
              <button onClick={saveFolder} className="nk-btn nk-btn-primary px-3">+</button>
            </div>
          </div>

          <div className="flex gap-2 mb-4">
            <input
              value={query}
              onChange={e => setQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && search()}
              placeholder="Search title, host, username"
              className="flex-1 input-field min-w-0"
            />
            <button onClick={search} className="nk-btn nk-btn-ghost border border-border shrink-0">
              <i className="fas fa-search sm:hidden" aria-hidden="true"></i>
              <span className="hidden sm:inline">Search</span>
            </button>
          </div>

          <div className="space-y-3">
            {items.map(item => (
              <div
                key={item.id}
                className={`nk-card p-4 flex flex-col gap-3 sm:flex-row sm:items-center ${draggingId === item.id ? 'opacity-50' : ''}`}
              >
                <div className="flex items-start gap-3 min-w-0 flex-1">
                  <button
                    type="button"
                    draggable
                    title="Drag onto a folder"
                    aria-label="Drag to folder"
                    onDragStart={e => {
                      e.dataTransfer.setData('text/plain', item.id);
                      e.dataTransfer.effectAllowed = 'move';
                      setDraggingId(item.id);
                    }}
                    onDragEnd={() => { setDraggingId(null); setDropTarget(null); }}
                    className="hidden sm:inline-flex shrink-0 px-1 pt-0.5 text-text-secondary cursor-grab active:cursor-grabbing hover:text-text"
                  >
                    <i className="fas fa-grip-vertical" aria-hidden="true"></i>
                  </button>
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-text truncate">{item.title} {item.host ? <span className="text-text-secondary font-normal">({item.host})</span> : null}</p>
                    <p className="text-xs text-text-secondary truncate">
                      {item.type}
                      {item.username ? ` · ${item.username}` : ''}
                      {!folderId && item.folderId ? ` · ${folderName(item.folderId) || 'folder'}` : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <FolderSelect
                    value={item.folderId}
                    folders={folders}
                    onChange={id => moveItem(item, id)}
                    noneLabel="No folder"
                    className="nk-select-chip"
                    ariaLabel={`Move ${item.title}`}
                  />
                  <div className="flex gap-1 ml-auto shrink-0">
                    {item.password && (
                      <button onClick={() => copy(item.password)} className="p-2 text-sm border border-border rounded-xl" title="Copy password">
                        <i className="fas fa-copy"></i>
                      </button>
                    )}
                    {item.privateKey && (
                      <button onClick={() => downloadPem(item)} className="p-2 text-sm border border-border rounded-xl" title="Download .pem">
                        <i className="fas fa-download"></i>
                      </button>
                    )}
                    <button onClick={() => { setEditing({ ...item }); setShowSecret(false); }} className="p-2 text-sm border border-border rounded-xl" title="Edit">
                      <i className="fas fa-pen"></i>
                    </button>
                    <button onClick={() => removeItem(item.id)} className="p-2 text-sm text-danger" title="Delete">
                      <i className="fas fa-trash"></i>
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {items.length === 0 && (
              <p className="text-text-secondary text-[15px] py-8">
                {selectedFolder
                  ? `Nothing in ${selectedFolder.name} yet. New in ${selectedFolder.name} lands here — or move a secret from All items.`
                  : 'No vault items.'}
              </p>
            )}
          </div>
        </div>
      </div>

      {editing && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-end sm:items-center justify-center p-0 sm:p-6">
          <div className="nk-card w-full max-w-lg rounded-t-[22px] sm:rounded-[22px] p-6 max-h-[90vh] overflow-y-auto">
            <h3 className="text-xl font-semibold tracking-tight mb-4">{editing.id ? 'Edit item' : 'New item'}</h3>
            <div className="space-y-3">
              <select
                value={editing.type}
                onChange={e => setEditing({ ...editing, type: e.target.value as VaultItemType })}
                className="w-full px-3 py-2 border border-border rounded-lg bg-background"
              >
                <option value="password">Password</option>
                <option value="ssh_key">SSH key</option>
                <option value="api_token">API token</option>
                <option value="note">Secure note</option>
              </select>
              <FolderSelect
                value={editing.folderId}
                folders={folders}
                onChange={id => setEditing({ ...editing, folderId: id })}
                noneLabel="No folder"
                className="w-full px-3 py-2 border border-border rounded-lg bg-background"
                ariaLabel="Save in folder"
              />
              <input value={editing.title} onChange={e => setEditing({ ...editing, title: e.target.value })} placeholder="Title" className="w-full px-3 py-2 border border-border rounded-lg bg-background" />
              <input value={editing.username || ''} onChange={e => setEditing({ ...editing, username: e.target.value })} placeholder="Username" className="w-full px-3 py-2 border border-border rounded-lg bg-background" />
              <input value={editing.host || ''} onChange={e => setEditing({ ...editing, host: e.target.value })} placeholder="Host" className="w-full px-3 py-2 border border-border rounded-lg bg-background" />
              <input value={editing.url || ''} onChange={e => setEditing({ ...editing, url: e.target.value })} placeholder="URL" className="w-full px-3 py-2 border border-border rounded-lg bg-background" />
              {(editing.type === 'password' || editing.type === 'api_token') && (
                <div className="flex gap-2">
                  <input
                    type={showSecret ? 'text' : 'password'}
                    value={editing.password || editing.secret || ''}
                    onChange={e => editing.type === 'api_token'
                      ? setEditing({ ...editing, secret: e.target.value })
                      : setEditing({ ...editing, password: e.target.value })}
                    placeholder={editing.type === 'api_token' ? 'Token' : 'Password'}
                    className="flex-1 px-3 py-2 border border-border rounded-lg bg-background"
                  />
                  <button onClick={() => setShowSecret(!showSecret)} className="px-3 border border-border rounded-lg">{showSecret ? 'Hide' : 'Show'}</button>
                  {editing.type === 'password' && (
                    <button onClick={() => setEditing({ ...editing, password: generatePassword() })} className="px-3 border border-border rounded-lg">Gen</button>
                  )}
                </div>
              )}
              {editing.type === 'ssh_key' && (
                <textarea
                  value={editing.privateKey || ''}
                  onChange={e => setEditing({ ...editing, privateKey: e.target.value })}
                  rows={8}
                  className="w-full px-3 py-2 border border-border rounded-lg font-mono text-sm bg-background"
                  placeholder="-----BEGIN OPENSSH PRIVATE KEY-----"
                />
              )}
              {editing.type === 'note' && (
                <textarea
                  value={editing.extraJson || ''}
                  onChange={e => setEditing({ ...editing, extraJson: e.target.value })}
                  rows={6}
                  className="w-full px-3 py-2 border border-border rounded-lg bg-background"
                  placeholder="Secure note"
                />
              )}
            </div>
            <div className="flex gap-2 mt-6">
              <button onClick={() => setEditing(null)} className="flex-1 nk-btn nk-btn-ghost border border-border">Cancel</button>
              <button onClick={saveItem} className="flex-1 nk-btn nk-btn-primary">Save</button>
            </div>
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => { setEditing(emptyItem(folderId)); setShowSecret(false); }}
        className="sm:hidden z-40 w-14 h-14 p-0 rounded-[28px] nk-btn nk-btn-primary shadow-lg"
        style={{ position: 'fixed', right: 20, bottom: 24, left: 'auto', top: 'auto' }}
        aria-label={selectedFolder ? `New in ${selectedFolder.name}` : 'New item'}
      >
        <i className="fas fa-plus" aria-hidden="true"></i>
      </button>
    </PageShell>
  );
};
