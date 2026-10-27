import React from 'react';

type Priority = 'high' | 'medium' | 'low';

interface PriorityBadgeProps {
  priority: Priority | string;
  className?: string;
}

const styles: Record<string, string> = {
  high: 'text-danger',
  medium: 'text-primary',
  low: 'text-text-secondary',
};

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority, className = '' }) => (
  <span
    className={`text-[11px] font-medium capitalize tracking-wide ${styles[priority] ?? styles.medium} ${className}`}
  >
    {priority}
  </span>
);
