import React from 'react';

interface CogitorMarkProps {
  size?: number;
  className?: string;
}

/** Hex-C Cogitor mark. Outer C follows currentColor; inner hex is brand indigo. */
export const CogitorMark: React.FC<CogitorMarkProps> = ({ size = 28, className = '' }) => (
  <svg
    viewBox="0 0 100 100"
    width={size}
    height={size}
    className={className}
    aria-hidden="true"
  >
    <path
      d="M93.3,33V25L50,0 6.7,25v50L50,100l43.3,-25v-8H75.1L50,81.5 22.7,65.75v-31.5L50,18.5 75.1,33z"
      fill="currentColor"
    />
    <path d="M50,30l17.3,10v20L50,70 32.7,60V40z" fill="#6E8BFF" />
  </svg>
);
