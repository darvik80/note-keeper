import React from 'react';
import { Todo } from '../types';
import { PriorityBadge } from './PriorityBadge';
import { MarkdownPreview } from './MarkdownPreview';

interface TodoCardProps {
  todo: Todo;
  onNavigate: () => void;
  onToggleComplete?: (e: React.MouseEvent) => void;
  onToggleFavorite?: (e: React.MouseEvent) => void;
  onArchive?: (e: React.MouseEvent) => void;
  onDelete?: (e: React.MouseEvent) => void;
  variant?: 'list' | 'readonly';
}

export const TodoCard: React.FC<TodoCardProps> = ({
  todo,
  onNavigate,
  onToggleComplete,
  onToggleFavorite,
  onArchive,
  onDelete,
  variant = 'list',
}) => {
  const isReadonly = variant === 'readonly';

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (isReadonly && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      onNavigate();
    }
  };

  if (isReadonly) {
    return (
      <div
        role="button"
        tabIndex={0}
        onClick={onNavigate}
        onKeyDown={handleKeyDown}
        className="bg-surface rounded-xl p-6 shadow-sm border border-border hover:shadow-md transition-shadow cursor-pointer"
      >
        <div className="flex items-start justify-between mb-2">
          <h4 className="font-bold text-text text-lg flex-1">{todo.title}</h4>
          <i className="fas fa-star text-yellow-500 shrink-0" aria-label="Favorite"></i>
        </div>
        {todo.description && (
          <MarkdownPreview content={todo.description} maxLines={3} className="mb-3" />
        )}
        {todo.tags.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {todo.tags.map(tag => (
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
      className={`bg-surface rounded-xl p-6 shadow-sm border border-border hover:shadow-md transition-all overflow-hidden ${
        todo.completed ? 'opacity-60' : ''
      }`}
    >
      <div className="flex items-start gap-4">
        {onToggleComplete && (
          <button
            onClick={onToggleComplete}
            className={`mt-1 w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors shrink-0 ${
              todo.completed
                ? 'bg-primary border-primary text-white'
                : 'border-border hover:border-primary'
            }`}
            aria-label={todo.completed ? 'Mark incomplete' : 'Mark complete'}
          >
            {todo.completed && <i className="fas fa-check text-xs" aria-hidden="true"></i>}
          </button>
        )}

        <div className="flex-1 cursor-pointer min-w-0" onClick={onNavigate}>
          <div className="flex items-start justify-between mb-2 gap-2">
            <h3 className={`font-bold text-lg ${todo.completed ? 'line-through text-text-secondary' : 'text-text'}`}>
              {todo.title}
            </h3>
            <div className="flex items-center gap-1 shrink-0">
              <PriorityBadge priority={todo.priority} />
              {todo.sharedWith && todo.sharedWith !== '[]' && (
                <i className="fas fa-share-alt text-green-500 text-sm" title="Shared" aria-label="Shared"></i>
              )}
              {onToggleFavorite && (
                <button
                  onClick={onToggleFavorite}
                  className="hover:scale-110 transition-transform p-1"
                  aria-label={todo.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                >
                  <i className={`fas fa-star ${todo.isFavorite ? 'text-yellow-500' : 'text-text-secondary/40'}`}></i>
                </button>
              )}
              {onArchive && (
                <button
                  onClick={onArchive}
                  className="p-1 text-text-secondary hover:text-text transition-colors"
                  title="Archive"
                  aria-label="Archive todo"
                >
                  <i className="fas fa-box-archive"></i>
                </button>
              )}
              {onDelete && (
                <button
                  onClick={onDelete}
                  className="p-1 text-red-500 hover:text-red-600 transition-colors"
                  title="Delete"
                  aria-label="Delete todo"
                >
                  <i className="fas fa-trash"></i>
                </button>
              )}
            </div>
          </div>

          {todo.description && (
            <MarkdownPreview content={todo.description} maxLines={3} className="mb-3" />
          )}

          <div className="flex items-center gap-4 text-sm text-text-secondary flex-wrap">
            {todo.dueDate && (
              <span>
                <i className="fas fa-calendar mr-1" aria-hidden="true"></i>
                {new Date(todo.dueDate).toLocaleDateString()}
              </span>
            )}
            {todo.location && (
              <span>
                <i className="fas fa-location-dot mr-1" aria-hidden="true"></i>
                {todo.location.address}
              </span>
            )}
            {todo.schedule && todo.schedule.repeat !== 'none' && (
              <span>
                <i className="fas fa-repeat mr-1" aria-hidden="true"></i>
                {todo.schedule.repeat}
              </span>
            )}
          </div>

          {todo.tags.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-3">
              {todo.tags.map(tag => (
                <span key={tag} className="text-xs bg-hover px-2 py-1 rounded">
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
