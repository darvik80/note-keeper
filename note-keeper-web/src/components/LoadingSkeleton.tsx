import React from 'react';

const pulse = 'animate-pulse bg-hover rounded';

export const CardGridSkeleton: React.FC<{ count?: number }> = ({ count = 6 }) => (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
    {Array.from({ length: count }).map((_, i) => (
      <div key={i} className="bg-surface rounded-xl p-5 lg:p-6 border border-border">
        <div className={`h-5 w-3/4 mb-4 ${pulse}`}></div>
        <div className={`h-3 w-full mb-2 ${pulse}`}></div>
        <div className={`h-3 w-full mb-2 ${pulse}`}></div>
        <div className={`h-3 w-2/3 mb-4 ${pulse}`}></div>
        <div className="flex justify-between">
          <div className={`h-5 w-20 ${pulse}`}></div>
          <div className={`h-5 w-14 ${pulse}`}></div>
        </div>
      </div>
    ))}
  </div>
);

export const TodoListSkeleton: React.FC<{ count?: number }> = ({ count = 4 }) => (
  <div className="space-y-4">
    {Array.from({ length: count }).map((_, i) => (
      <div key={i} className="bg-surface rounded-xl p-6 border border-border">
        <div className="flex items-start gap-4">
          <div className={`w-6 h-6 rounded-full shrink-0 ${pulse}`}></div>
          <div className="flex-1">
            <div className={`h-5 w-1/2 mb-3 ${pulse}`}></div>
            <div className={`h-3 w-full mb-2 ${pulse}`}></div>
            <div className={`h-3 w-3/4 ${pulse}`}></div>
          </div>
        </div>
      </div>
    ))}
  </div>
);

export const StatCardsSkeleton: React.FC = () => (
  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-6 mb-6 lg:mb-8">
    {Array.from({ length: 4 }).map((_, i) => (
      <div key={i} className="bg-surface rounded-xl p-4 lg:p-6 border border-border">
        <div className="flex items-center justify-between mb-3">
          <div className={`w-10 h-10 lg:w-12 lg:h-12 rounded-lg ${pulse}`}></div>
          <div className={`h-8 w-10 ${pulse}`}></div>
        </div>
        <div className={`h-4 w-24 ${pulse}`}></div>
      </div>
    ))}
  </div>
);
