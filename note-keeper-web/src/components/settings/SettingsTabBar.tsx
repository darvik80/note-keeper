import React from 'react';

export type SettingsTab = 'integrations' | 'shortcuts' | 'daily-report' | 'tokens' | 'api' | 'backup';

interface SettingsTabBarProps {
  activeTab: SettingsTab;
  onTabChange: (tab: SettingsTab) => void;
}

const TABS: { id: SettingsTab; label: string; short: string }[] = [
  { id: 'integrations', label: 'Integrations', short: 'Connect' },
  { id: 'shortcuts', label: 'Shortcuts', short: 'Keys' },
  { id: 'daily-report', label: 'Daily Report', short: 'Report' },
  { id: 'tokens', label: 'API tokens', short: 'Tokens' },
  { id: 'api', label: 'API docs', short: 'API' },
  { id: 'backup', label: 'Backup', short: 'Backup' },
];

export const SettingsTabBar: React.FC<SettingsTabBarProps> = ({ activeTab, onTabChange }) => (
  <div className="flex gap-1 mb-8 p-1 rounded-full bg-hover overflow-x-auto scrollbar-hide">
    {TABS.map(tab => (
      <button
        key={tab.id}
        onClick={() => onTabChange(tab.id)}
        className={`px-3 sm:px-4 py-2 text-[13px] font-medium rounded-full whitespace-nowrap shrink-0 transition-colors duration-[180ms] ${
          activeTab === tab.id
            ? 'bg-surface text-primary'
            : 'text-text-secondary hover:text-text'
        }`}
      >
        <span className="hidden sm:inline">{tab.label}</span>
        <span className="sm:hidden">{tab.short}</span>
      </button>
    ))}
  </div>
);
