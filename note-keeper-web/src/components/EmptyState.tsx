import React from 'react';

interface EmptyStateProps {
  icon: string;
  message: string;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({ icon, message, action }) => (
  <div className="text-center py-16">
    <i className={`fas ${icon} text-6xl text-text-secondary/30 mb-4`} aria-hidden="true"></i>
    <p className="text-text-secondary text-lg">{message}</p>
    {action && <div className="mt-4">{action}</div>}
  </div>
);
