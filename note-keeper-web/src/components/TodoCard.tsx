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
  const isRecurring = !!todo.schedule && todo.schedule.repeat !== 'none';
  const doneThisCycle = isRecurring && todo.completed;
  const formatDay = (value: Date | string) => new Date(value).toLocaleDateString();
  const formatDayTime = (value: Date | string) =>
    new Date(value).toLocaleString(undefined, { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });

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
        className="nk-card nk-card-hover p-5 cursor-pointer"
      >
        <div className="flex items-start justify-between mb-2">
          <h4 className="font-semibold text-text text-[17px] flex-1 tracking-tight">{todo.title}</h4>
          <i className="fas fa-star text-primary shrink-0" aria-label="Favorite"></i>
        </div>
        {todo.description && (
          <MarkdownPreview content={todo.description} maxLines={3} className="mb-3" />
        )}
        {todo.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {todo.tags.map(tag => (
              <span key={tag} className="nk-tag">#{tag}</span>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div
      className={`nk-card nk-card-hover p-5 overflow-hidden ${
        !isRecurring && todo.completed ? 'opacity-60' : ''
      }`}
    >
      <div className="flex items-start gap-4">
        {onToggleComplete && (
          <button
            onClick={onToggleComplete}
            className={`mt-1 w-5 h-5 rounded-full border flex items-center justify-center transition-colors duration-[180ms] shrink-0 ${
              (isRecurring ? doneThisCycle : todo.completed)
                ? 'bg-primary border-primary text-[#0B0E11]'
                : 'border-border hover:border-primary'
            }`}
            aria-label={doneThisCycle || todo.completed ? 'Mark incomplete' : 'Mark complete'}
          >
            {(isRecurring ? doneThisCycle : todo.completed) && <i className="fas fa-check text-[10px]" aria-hidden="true"></i>}
          </button>
        )}

        <div className="flex-1 cursor-pointer min-w-0" onClick={onNavigate}>
          <div className="flex items-start justify-between mb-1.5 gap-2">
            <h3 className={`font-semibold text-[17px] tracking-tight ${
              (isRecurring ? doneThisCycle : todo.completed) ? 'line-through text-text-secondary' : 'text-text'
            }`}>
              {todo.title}
            </h3>
            <div className="flex items-center gap-0.5 shrink-0">
              <PriorityBadge priority={todo.priority} />
              {todo.sharedWith && todo.sharedWith !== '[]' && (
                <i className="fas fa-share-alt text-secondary text-sm p-1" title="Shared" aria-label="Shared"></i>
              )}
              {onToggleFavorite && (
                <button
                  onClick={onToggleFavorite}
                  className="p-1.5 rounded-lg hover:bg-background transition-colors duration-[180ms]"
                  aria-label={todo.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                >
                  <i className={`fas fa-star ${todo.isFavorite ? 'text-primary' : 'text-text-secondary/40'}`}></i>
                </button>
              )}
              {onArchive && (
                <button
                  onClick={onArchive}
                  className="p-1.5 text-text-secondary hover:text-text rounded-lg hover:bg-background transition-colors duration-[180ms]"
                  title="Archive"
                  aria-label="Archive todo"
                >
                  <i className="fas fa-box-archive"></i>
                </button>
              )}
              {onDelete && (
                <button
                  onClick={onDelete}
                  className="p-1.5 text-danger hover:opacity-80 rounded-lg hover:bg-background transition-colors duration-[180ms]"
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

          <div className="flex items-center gap-4 text-[13px] text-text-secondary flex-wrap">
            {todo.dueDate && (
              <span>
                <i className="fas fa-calendar mr-1" aria-hidden="true"></i>
                {new Date(todo.dueDate).toLocaleDateString()}
              </span>
            )}
            {isRecurring && (
              <span className="flex flex-wrap items-center gap-x-3 gap-y-1">
                <span>
                  <i className="fas fa-repeat mr-1" aria-hidden="true"></i>
                  {todo.schedule!.repeat}
                </span>
                {todo.lastCompletedAt && (
                  <span className="text-secondary" title="Last completed">
                    <i className="fas fa-check-circle mr-1" aria-hidden="true"></i>
                    last: {formatDay(todo.lastCompletedAt)}
                  </span>
                )}
                {todo.reminder && (
                  <span title="Next reminder">
                    <i className="fas fa-bell mr-1" aria-hidden="true"></i>
                    next: {formatDayTime(todo.reminder)}
                  </span>
                )}
                {doneThisCycle && <span className="text-secondary">done this cycle</span>}
              </span>
            )}
          </div>

          {todo.tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mt-3">
              {todo.tags.map(tag => (
                <span key={tag} className="nk-tag">#{tag}</span>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
