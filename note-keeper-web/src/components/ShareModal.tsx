import React, { useEffect, useState } from 'react';
import { User } from '../types';
import { Modal } from './Modal';
import { useToast } from '../contexts/ToastContext';

interface ShareModalProps {
  isOpen: boolean;
  onClose: () => void;
  resourceId: string;
  resourceType: 'note' | 'todo';
  ownerId: string;
  sharedWith?: string;
  onShareSuccess?: () => void;
}

export const ShareModal: React.FC<ShareModalProps> = ({
  isOpen,
  onClose,
  resourceId,
  resourceType,
  sharedWith = '[]',
  onShareSuccess,
}) => {
  const { toast } = useToast();
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [sharedUsers, setSharedUsers] = useState<string[]>([]);
  const [sharedUserDetails, setSharedUserDetails] = useState<Map<string, User>>(new Map());
  const [loading, setLoading] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      parseSharedWith(sharedWith);
      setSearchQuery('');
      setUsers([]);
      setError('');
    }
  }, [isOpen, sharedWith]);

  useEffect(() => {
    if (sharedUsers.length > 0) {
      loadUserDetails(sharedUsers);
    } else {
      setSharedUserDetails(new Map());
    }
  }, [sharedUsers]);

  const parseSharedWith = (sharedWithStr: string) => {
    try {
      const parsed = JSON.parse(sharedWithStr);
      setSharedUsers(Array.isArray(parsed) ? parsed : []);
    } catch {
      setSharedUsers([]);
    }
  };

  const loadUserDetails = async (userIds: string[]) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/users/batch', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(userIds),
      });
      if (!response.ok) throw new Error('Failed to load user details');
      const data: User[] = await response.json();
      const userMap = new Map<string, User>();
      data.forEach(user => userMap.set(user.id, user));
      setSharedUserDetails(userMap);
    } catch {
      const userMap = new Map<string, User>();
      userIds.forEach(id => userMap.set(id, { id, name: id, email: '' } as User));
      setSharedUserDetails(userMap);
    }
  };

  const searchUsers = async (query: string) => {
    if (!query.trim()) {
      setUsers([]);
      return;
    }
    setSearchLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/v1/users/search?query=${encodeURIComponent(query)}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error('Failed to search users');
      setUsers(await response.json());
    } catch (err: any) {
      setError(err.message || 'Failed to search users');
      setUsers([]);
    } finally {
      setSearchLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery) searchUsers(searchQuery);
      else setUsers([]);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const updateSharedFromResponse = (result: any, fallback: string[]) => {
    const updatedSharedWith = result.sharedWith || result.shared_with;
    if (updatedSharedWith) {
      try {
        const parsed = JSON.parse(updatedSharedWith);
        setSharedUsers(Array.isArray(parsed) ? parsed : fallback);
      } catch {
        setSharedUsers(fallback);
      }
    } else {
      setSharedUsers(fallback);
    }
  };

  const handleShare = async (userId: string) => {
    if (sharedUsers.includes(userId)) return;
    setLoading(true);
    setError('');
    try {
      const endpoint = resourceType === 'note'
        ? `/api/v1/notes/${resourceId}/share?userId=${userId}`
        : `/api/v1/todos/${resourceId}/share?userId=${userId}`;
      const token = localStorage.getItem('token');
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error(await response.text() || 'Failed to share');
      const result = await response.json();
      updateSharedFromResponse(result, [...sharedUsers, userId]);
      toast.success('Shared successfully');
      onShareSuccess?.();
    } catch (err: any) {
      setError(err.message || 'Failed to share');
    } finally {
      setLoading(false);
    }
  };

  const handleUnshare = async (userId: string) => {
    setLoading(true);
    setError('');
    try {
      const endpoint = resourceType === 'note'
        ? `/api/v1/notes/${resourceId}/share?userId=${userId}`
        : `/api/v1/todos/${resourceId}/share?userId=${userId}`;
      const token = localStorage.getItem('token');
      const response = await fetch(endpoint, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error(await response.text() || 'Failed to unshare');
      const result = await response.json();
      updateSharedFromResponse(result, sharedUsers.filter(id => id !== userId));
      toast.info('Access removed');
      onShareSuccess?.();
    } catch (err: any) {
      setError(err.message || 'Failed to unshare');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Share ${resourceType === 'note' ? 'Note' : 'Todo'}`}
      footer={
        <button
          onClick={onClose}
          className="w-full px-4 py-2 border border-border rounded-lg hover:bg-hover transition-colors text-text"
        >
          Close
        </button>
      }
    >
      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div className="mb-4">
        <label className="block text-sm font-medium text-text mb-2">Search users</label>
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Search by email or name"
          className="input-field"
          disabled={searchLoading}
        />
        {searchLoading && <p className="text-xs text-text-secondary mt-1">Searching...</p>}
      </div>

      {users.length > 0 && (
        <div className="mb-4">
          <label className="block text-sm font-medium text-text mb-2">Found users</label>
          <div className="space-y-2 max-h-40 overflow-y-auto">
            {users.map(user => (
              <div key={user.id} className="flex items-center justify-between p-2 bg-secondary/10 rounded-lg">
                <div>
                  <p className="text-sm font-medium text-text">{user.name}</p>
                  <p className="text-xs text-text-secondary">{user.email}</p>
                </div>
                <button
                  onClick={() => handleShare(user.id)}
                  disabled={loading || sharedUsers.includes(user.id)}
                  className="px-3 py-1 bg-primary text-white text-sm rounded-lg hover:bg-primary/90 disabled:opacity-50"
                >
                  {sharedUsers.includes(user.id) ? 'Added' : 'Add'}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-text mb-2">Currently shared with</label>
        {sharedUsers.length === 0 ? (
          <p className="text-sm text-text-secondary">Not shared with anyone</p>
        ) : (
          <div className="space-y-2 max-h-48 overflow-y-auto">
            {sharedUsers.map(userId => {
              const user = sharedUserDetails.get(userId);
              return (
                <div key={userId} className="flex items-center justify-between p-2 bg-hover rounded-lg">
                  <div>
                    <p className="text-sm font-medium text-text">{user?.name || userId}</p>
                    {user?.email && <p className="text-xs text-text-secondary">{user.email}</p>}
                  </div>
                  <button
                    onClick={() => handleUnshare(userId)}
                    disabled={loading}
                    className="text-red-500 hover:text-red-600 text-sm disabled:opacity-50"
                  >
                    Remove
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </Modal>
  );
};
