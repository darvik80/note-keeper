import React from 'react';

export type SettingsTab = 'integrations' | 'shortcuts' | 'api' | 'backup';

interface SettingsTabBarProps {
  activeTab: SettingsTab;
  onTabChange: (tab: SettingsTab) => void;
}

const TABS: { id: SettingsTab; label: string; icon: string }[] = [
  { id: 'integrations', label: 'Integrations', icon: 'fa-plug' },
  { id: 'shortcuts', label: 'Shortcuts', icon: 'fa-keyboard' },
  { id: 'api', label: 'API Documentation', icon: 'fa-code' },
  { id: 'backup', label: 'Backup', icon: 'fa-database' },
];

export const SettingsTabBar: React.FC<SettingsTabBarProps> = ({ activeTab, onTabChange }) => (
  <div className="flex gap-1 sm:gap-4 mb-8 border-b border-border overflow-x-auto scrollbar-hide">
    {TABS.map(tab => (
      <button
        key={tab.id}
        onClick={() => onTabChange(tab.id)}
        className={`px-3 sm:px-6 py-3 font-medium transition-colors whitespace-nowrap shrink-0 ${
          activeTab === tab.id
            ? 'text-primary border-b-2 border-primary'
            : 'text-text-secondary hover:text-text'
        }`}
      >
        <i className={`fas ${tab.icon} mr-2`}></i>
        <span className="hidden sm:inline">{tab.label}</span>
        <span className="sm:hidden">{tab.label.split(' ')[0]}</span>
      </button>
    ))}
  </div>
);
