/**
 * @module ShareModal
 * @category Components
 * Modal dialog for sharing a note or todo with other registered users.
 *
 * Features:
 * - Debounced user search by name or email (300 ms delay)
 * - One-click share / unshare via the backend share endpoints
 * - Displays the current list of users the resource is already shared with,
 *   resolving their full profiles via `POST /api/v1/users/batch`
 */
import React, { useState, useEffect } from 'react';
import { User } from '../types';

/** Props for {@link ShareModal}. */
interface ShareModalProps {
  /** Controls modal visibility. */
  isOpen: boolean;
  /** Called when the user closes the modal. */
  onClose: () => void;
  /** UUID of the note or todo being shared. */
  resourceId: string;
  /** Determines which share endpoint to call. */
  resourceType: 'note' | 'todo';
  /** UUID of the resource owner (used for display). */
  ownerId: string;
  /**
   * JSON-encoded array of user UUIDs already sharing this resource.
   * Defaults to `"[]"`.
   */
  sharedWith?: string;
  /** Called after a successful share or unshare action so the parent can refresh. */
  onShareSuccess?: () => void;
}

/**
 * Modal for managing resource sharing with other users.
 * @param props - See {@link ShareModalProps}.
 */
export const ShareModal: React.FC<ShareModalProps> = ({
  isOpen,
  onClose,
  resourceId,
  resourceType,
  ownerId,
  sharedWith = '[]',
  onShareSuccess
}) => {
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
    }
  }, [isOpen, sharedWith]);

  // Load user details when sharedUsers changes
  useEffect(() => {
    if (sharedUsers.length > 0) {
      loadUserDetails(sharedUsers);
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
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(userIds)
      });

      if (!response.ok) {
        throw new Error('Failed to load user details');
      }

      const users: User[] = await response.json();
      const userMap = new Map<string, User>();
      users.forEach(user => userMap.set(user.id, user));
      setSharedUserDetails(userMap);
    } catch (err: any) {
      console.error('[ShareModal] Failed to load user details:', err);
      // Fallback: create placeholder user objects with just ID
      const userMap = new Map<string, User>();
      userIds.forEach(id => {
        userMap.set(id, { id, name: id, email: '' } as User);
      });
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
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to search users');
      }

      const data = await response.json();
      setUsers(data);
    } catch (err: any) {
      setError(err.message || 'Failed to search users');
      setUsers([]);
    } finally {
      setSearchLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchQuery) {
        searchUsers(searchQuery);
      } else {
        setUsers([]);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const handleShare = async (userId: string) => {
    if (sharedUsers.includes(userId)) {
      return;
    }

    setLoading(true);
    setError('');

    try {
      const endpoint = resourceType === 'note' 
        ? `/api/v1/notes/${resourceId}/share?userId=${userId}`
        : `/api/v1/todos/${resourceId}/share?userId=${userId}`;

      const token = localStorage.getItem('token');
      console.log('[ShareModal] Sharing resource:', resourceId, 'with user:', userId);
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to share');
      }
      
      const result = await response.json();
      console.log('[ShareModal] Share response:', result);
      
      // Update sharedUsers from response
      const updatedSharedWith = result.sharedWith || result.shared_with;
      if (updatedSharedWith) {
        try {
          const parsed = JSON.parse(updatedSharedWith);
          setSharedUsers(Array.isArray(parsed) ? parsed : []);
        } catch (e) {
          console.error('[ShareModal] Failed to parse sharedWith:', updatedSharedWith);
          setSharedUsers([...sharedUsers, userId]);
        }
      } else {
        setSharedUsers([...sharedUsers, userId]);
      }
      
      onShareSuccess?.();
    } catch (err: any) {
      console.error('[ShareModal] Share error:', err);
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
      console.log('[ShareModal] Unsharing resource:', resourceId, 'from user:', userId);
      const response = await fetch(endpoint, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to unshare');
      }
      
      const result = await response.json();
      console.log('[ShareModal] Unshare response:', result);
      
      // Update sharedUsers from response
      const updatedSharedWith = result.sharedWith || result.shared_with;
      if (updatedSharedWith) {
        try {
          const parsed = JSON.parse(updatedSharedWith);
          setSharedUsers(Array.isArray(parsed) ? parsed : []);
        } catch (e) {
          console.error('[ShareModal] Failed to parse sharedWith:', updatedSharedWith);
          setSharedUsers(sharedUsers.filter(id => id !== userId));
        }
      } else {
        setSharedUsers(sharedUsers.filter(id => id !== userId));
      }
      
      onShareSuccess?.();
    } catch (err: any) {
      console.error('[ShareModal] Unshare error:', err);
      setError(err.message || 'Failed to unshare');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold">Share {resourceType === 'note' ? 'Note' : 'Todo'}</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            <i className="fas fa-times"></i>
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 text-red-600 rounded-lg text-sm">
            {error}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Search users
          </label>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by email or name"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            disabled={searchLoading}
          />
          {searchLoading && (
            <p className="text-xs text-gray-500 mt-1">Searching...</p>
          )}
        </div>

        {users.length > 0 && (
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Found users:
            </label>
            <div className="space-y-2 max-h-40 overflow-y-auto">
              {users.map(user => (
                <div
                  key={user.id}
                  className="flex items-center justify-between p-2 bg-blue-50 rounded"
                >
                  <div>
                    <p className="text-sm font-medium">{user.name}</p>
                    <p className="text-xs text-gray-500">{user.email}</p>
                  </div>
                  <button
                    onClick={() => handleShare(user.id)}
                    disabled={loading || sharedUsers.includes(user.id)}
                    className="px-3 py-1 bg-primary text-white text-sm rounded hover:bg-primary/90 disabled:opacity-50"
                  >
                    {sharedUsers.includes(user.id) ? 'Added' : 'Add'}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Currently shared with:
          </label>
          {sharedUsers.length === 0 ? (
            <p className="text-sm text-gray-500">Not shared with anyone</p>
          ) : (
            <div className="space-y-2">
              {sharedUsers.map(userId => {
                const user = sharedUserDetails.get(userId);
                return (
                  <div
                    key={userId}
                    className="flex items-center justify-between p-2 bg-gray-50 rounded"
                  >
                    <div>
                      <p className="text-sm font-medium">{user?.name || userId}</p>
                      {user?.email && (
                        <p className="text-xs text-gray-500">{user.email}</p>
                      )}
                    </div>
                    <button
                      onClick={() => handleUnshare(userId)}
                      disabled={loading}
                      className="text-red-600 hover:text-red-800 text-sm disabled:opacity-50"
                    >
                      Remove
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="flex gap-2">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
