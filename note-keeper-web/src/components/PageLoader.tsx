import React from 'react';

export const PageLoader: React.FC = () => (
  <div className="flex flex-1 items-center justify-center bg-background min-h-0 w-full">
    <div className="flex flex-col items-center gap-3 text-text-secondary">
      <i className="fas fa-spinner fa-spin text-2xl text-primary" aria-hidden="true"></i>
      <span className="text-sm">Loading...</span>
    </div>
  </div>
);
