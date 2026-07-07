import React from 'react';
import {ErrorBanner} from './ErrorBanner';

interface PageShellProps {
  error?: string | null;
  onDismissError?: () => void;
  className?: string;
  children: React.ReactNode;
}

export const PageShell: React.FC<PageShellProps> = ({ error, onDismissError, className = '', children }) => (
  <div className={`flex flex-1 flex-col bg-background min-h-0 w-full overflow-hidden ${className}`}>
    {error && <ErrorBanner message={error} onDismiss={onDismissError} />}
    {children}
  </div>
);
