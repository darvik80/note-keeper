import React from 'react';

interface EmptyStateProps {
  icon: string;
  message: string;
  hint?: string;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({ icon, message, hint, action }) => (
  <div className="text-center py-20 px-6">
    <i className={`fas ${icon} text-xl text-text-secondary/50 mb-4`} aria-hidden="true"></i>
    <p className="text-[17px] font-medium text-text">{message}</p>
    {hint && <p className="mt-1 text-[15px] text-text-secondary">{hint}</p>}
    {action && <div className="mt-5">{action}</div>}
  </div>
);
