import React from 'react';
import { Note } from '../types';
import { PriorityBadge } from './PriorityBadge';
import { MarkdownPreview } from './MarkdownPreview';

interface NoteCardProps {
  note: Note;
  onClick: () => void;
  onToggleFavorite?: (e: React.MouseEvent) => void;
  onArchive?: (e: React.MouseEvent) => void;
  onDelete?: (e: React.MouseEvent) => void;
  variant?: 'grid' | 'compact' | 'readonly';
}

export const NoteCard: React.FC<NoteCardProps> = ({
  note,
  onClick,
  onToggleFavorite,
  onArchive,
  onDelete,
  variant = 'grid',
}) => {
  const isReadonly = variant === 'readonly';
  const isCompact = variant === 'compact';

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick();
    }
  };

  if (isCompact) {
    return (
      <div
        role="button"
        tabIndex={0}
        onClick={onClick}
        onKeyDown={handleKeyDown}
        className="p-4 border border-border rounded-lg hover:border-primary transition-colors cursor-pointer bg-surface"
      >
        <div className="flex items-start justify-between mb-2">
          <h4 className="font-semibold text-text text-sm lg:text-base pr-2">{note.title}</h4>
          {note.isFavorite && (
            <i className="fas fa-star text-yellow-500 text-sm lg:text-base shrink-0" aria-label="Favorite"></i>
          )}
        </div>
        {note.isEncrypted ? (
          <p className="text-sm text-text-secondary italic">
            <i className="fas fa-lock mr-1" aria-hidden="true"></i>
            Encrypted content
          </p>
        ) : (
          <MarkdownPreview content={note.content} maxLines={3} />
        )}
        {note.tags.length > 0 && (
          <div className="flex items-center gap-2 mt-2 flex-wrap">
            {note.tags.slice(0, 3).map(tag => (
              <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                #{tag}
              </span>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={handleKeyDown}
      className="bg-surface rounded-xl p-5 lg:p-6 shadow-sm border border-border hover:shadow-md transition-shadow cursor-pointer"
    >
      <div className="flex items-start justify-between mb-3">
        <h3 className="font-bold text-text text-base lg:text-lg flex-1 pr-2">{note.title}</h3>
        <div className="flex items-center gap-1 lg:gap-2 shrink-0">
          {note.isEncrypted && (
            <i className="fas fa-lock text-purple-500 text-sm" title="Encrypted" aria-label="Encrypted"></i>
          )}
          {note.sharedWith && note.sharedWith !== '[]' && (
            <i className="fas fa-share-alt text-green-500 text-sm" title="Shared" aria-label="Shared"></i>
          )}
          {(isReadonly && note.isFavorite) ? (
            <i className="fas fa-star text-yellow-500 text-sm" aria-label="Favorite"></i>
          ) : onToggleFavorite && (
            <button
              onClick={onToggleFavorite}
              className="hover:scale-110 transition-transform p-1"
              aria-label={note.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
            >
              <i className={`fas fa-star text-sm ${note.isFavorite ? 'text-yellow-500' : 'text-text-secondary'}`}></i>
            </button>
          )}
          {!isReadonly && onArchive && (
            <button
              onClick={onArchive}
              className="text-text-secondary hover:text-text p-1 transition-colors"
              title="Archive"
              aria-label="Archive note"
            >
              <i className="fas fa-box-archive text-sm"></i>
            </button>
          )}
          {!isReadonly && onDelete && (
            <button
              onClick={onDelete}
              className="text-red-500 hover:text-red-600 p-1 transition-colors"
              title="Delete"
              aria-label="Delete note"
            >
              <i className="fas fa-trash text-sm"></i>
            </button>
          )}
        </div>
      </div>

      {note.isEncrypted ? (
        <p className="text-sm text-text-secondary italic mb-4">
          <i className="fas fa-lock mr-1" aria-hidden="true"></i>
          Encrypted content
        </p>
      ) : (
        <MarkdownPreview content={note.content} maxLines={4} className="mb-4 lg:line-clamp-3" />
      )}

      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex flex-wrap gap-2">
          {note.tags.slice(0, 3).map(tag => (
            <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
              #{tag}
            </span>
          ))}
        </div>
        <PriorityBadge priority={note.priority} />
      </div>

      {note.subfolder && (
        <div className="mt-3 text-xs text-text-secondary truncate">
          <i className="fas fa-folder-tree mr-1" aria-hidden="true"></i>
          {note.folder}/{note.subfolder}
        </div>
      )}
    </div>
  );
};
