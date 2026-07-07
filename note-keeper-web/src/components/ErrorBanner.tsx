import React from 'react';

interface ErrorBannerProps {
  message: string;
  onDismiss?: () => void;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({ message, onDismiss }) => (
  <div
    role="alert"
    className="flex items-center gap-3 px-4 py-3 bg-red-500/10 border-b border-red-500/20 text-red-600 dark:text-red-400 text-sm"
  >
    <i className="fas fa-circle-exclamation shrink-0" aria-hidden="true"></i>
    <span className="flex-1">{message}</span>
    {onDismiss && (
      <button
        onClick={onDismiss}
        className="shrink-0 hover:opacity-70 transition-opacity p-1"
        aria-label="Dismiss error"
      >
        <i className="fas fa-times" aria-hidden="true"></i>
      </button>
    )}
  </div>
);
