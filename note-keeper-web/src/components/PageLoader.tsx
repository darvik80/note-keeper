import React from 'react';

export const PageLoader: React.FC = () => (
  <div className="flex flex-1 items-center justify-center bg-background min-h-0 w-full">
    <div className="w-6 h-6 rounded-full border-2 border-border border-t-primary animate-spin" aria-label="Loading" />
  </div>
);
