import React from 'react';

type Priority = 'high' | 'medium' | 'low';

interface PriorityBadgeProps {
  priority: Priority | string;
  className?: string;
}

const styles: Record<string, string> = {
  high: 'bg-red-500/15 text-red-600 dark:text-red-400',
  medium: 'bg-yellow-500/15 text-yellow-700 dark:text-yellow-400',
  low: 'bg-green-500/15 text-green-700 dark:text-green-400',
};

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority, className = '' }) => (
  <span
    className={`text-xs px-2 py-1 rounded whitespace-nowrap font-medium capitalize ${styles[priority] ?? styles.medium} ${className}`}
  >
    {priority}
  </span>
);
