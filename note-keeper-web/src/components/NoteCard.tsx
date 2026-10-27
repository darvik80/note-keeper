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
        className="p-4 rounded-[22px] hover:bg-hover transition-colors duration-[180ms] cursor-pointer"
      >
        <div className="flex items-start justify-between mb-1.5">
          <h4 className="font-semibold text-text text-[15px] pr-2">{note.title}</h4>
          {note.isFavorite && (
            <i className="fas fa-star text-primary text-xs shrink-0" aria-label="Favorite"></i>
          )}
        </div>
        {note.isEncrypted ? (
          <p className="text-[15px] text-text-secondary italic">
            <i className="fas fa-lock mr-1" aria-hidden="true"></i>
            Encrypted content
          </p>
        ) : (
          <MarkdownPreview content={note.content} maxLines={3} />
        )}
        {note.tags.length > 0 && (
          <div className="flex items-center gap-1.5 mt-2 flex-wrap">
            {note.tags.slice(0, 3).map(tag => (
              <span key={tag} className="nk-tag">#{tag}</span>
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
      className="nk-card nk-card-hover p-5 lg:p-6 cursor-pointer"
    >
      <div className="flex items-start justify-between mb-3">
        <h3 className="font-semibold text-text text-[17px] lg:text-xl flex-1 pr-2 tracking-tight">{note.title}</h3>
        <div className="flex items-center gap-0.5 shrink-0">
          {note.isEncrypted && (
            <i className="fas fa-lock text-mark text-sm p-1" title="Encrypted" aria-label="Encrypted"></i>
          )}
          {note.sharedWith && note.sharedWith !== '[]' && (
            <i className="fas fa-share-alt text-secondary text-sm p-1" title="Shared" aria-label="Shared"></i>
          )}
          {(isReadonly && note.isFavorite) ? (
            <i className="fas fa-star text-primary text-sm p-1" aria-label="Favorite"></i>
          ) : onToggleFavorite && (
            <button
              onClick={onToggleFavorite}
              className="p-1.5 rounded-lg hover:bg-background transition-colors duration-[180ms]"
              aria-label={note.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
            >
              <i className={`fas fa-star text-sm ${note.isFavorite ? 'text-primary' : 'text-text-secondary'}`}></i>
            </button>
          )}
          {!isReadonly && onArchive && (
            <button
              onClick={onArchive}
              className="text-text-secondary hover:text-text p-1.5 rounded-lg hover:bg-background transition-colors duration-[180ms]"
              title="Archive"
              aria-label="Archive note"
            >
              <i className="fas fa-box-archive text-sm"></i>
            </button>
          )}
          {!isReadonly && onDelete && (
            <button
              onClick={onDelete}
              className="text-danger hover:opacity-80 p-1.5 rounded-lg hover:bg-background transition-colors duration-[180ms]"
              title="Delete"
              aria-label="Delete note"
            >
              <i className="fas fa-trash text-sm"></i>
            </button>
          )}
        </div>
      </div>

      {note.isEncrypted ? (
        <p className="text-[15px] text-text-secondary italic mb-4">
          <i className="fas fa-lock mr-1" aria-hidden="true"></i>
          Encrypted content
        </p>
      ) : (
        <MarkdownPreview content={note.content} maxLines={4} className="mb-4 lg:line-clamp-3" />
      )}

      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex flex-wrap gap-1.5">
          {note.tags.slice(0, 3).map(tag => (
            <span key={tag} className="nk-tag">#{tag}</span>
          ))}
        </div>
        <PriorityBadge priority={note.priority} />
      </div>

      {note.subfolder && (
        <div className="mt-3 text-[11px] text-text-secondary truncate">
          <i className="fas fa-folder-tree mr-1" aria-hidden="true"></i>
          {note.folder}/{note.subfolder}
        </div>
      )}
    </div>
  );
};
