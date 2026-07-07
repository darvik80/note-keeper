/**
 * @module Settings
 * @category Pages
 * @description User settings page — integration config, keyboard shortcuts, theme, and backup.
 */
import React, {useEffect, useState} from 'react';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {SettingsTabBar, SettingsTab} from '../components/settings/SettingsTabBar';
import {ApiTab} from '../components/settings/ApiTab';
import {useToast} from '../contexts/ToastContext';
import {storage} from '../utils/storage';
import {IntegrationRequest, IntegrationResponse, Settings as SettingsType} from '../types';
import {api} from '../utils/api';

/** Settings page for configuring Telegram, DingTalk, email integrations, keyboard shortcuts, and backup. */
export const Settings: React.FC = () => {
  const { toast } = useToast();
  const [settings, setSettings] = useState<SettingsType>(storage.getSettings());
  const [saved, setSaved] = useState(false);
  const [activeTab, setActiveTab] = useState<SettingsTab>('integrations');
  const [telegramTestStatus, setTelegramTestStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');
  const [dingtalkTestStatus, setDingtalkTestStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);
  const [backupStatus, setBackupStatus] = useState<'idle' | 'exporting' | 'importing' | 'success' | 'error'>('idle');
  const [backupMessage, setBackupMessage] = useState('');
  const [backupSettings, setBackupSettings] = useState({
    enabled: false,
    cron: '0 0 2 * * *',
    retentionDays: 30
  });
  const [settingsSaved, setSettingsSaved] = useState(false);
  const [showTelegramToken, setShowTelegramToken] = useState(false);
  const [showTelegramChatId, setShowTelegramChatId] = useState(false);
  const [showDingtalkWebhook, setShowDingtalkWebhook] = useState(false);
  const [showDingtalkSecret, setShowDingtalkSecret] = useState(false);
  const [loadingSettings, setLoadingSettings] = useState(false);

  // Load settings from backend on mount; migrate from localStorage if backend is empty
  useEffect(() => {
    const loadSettings = async () => {
      setLoadingSettings(true);
      try {
        const backendSettings = await api.settings.get();
        const hasBackendData = backendSettings && (
          backendSettings.telegramBotToken ||
          backendSettings.telegramChatId ||
          backendSettings.dingtalkWebhook ||
          backendSettings.dingtalkSecret
        );

        if (hasBackendData) {
          // Use backend settings (already decrypted)
          setSettings(prev => ({
            ...prev,
            telegram: {
              enabled: !!backendSettings.telegramBotToken,
              botToken: backendSettings.telegramBotToken || '',
              chatId: backendSettings.telegramChatId || ''
            },
            dingtalk: {
              enabled: !!backendSettings.dingtalkWebhook,
              webhook: backendSettings.dingtalkWebhook || '',
              secret: backendSettings.dingtalkSecret || ''
            }
          }));
        } else {
          // Backend is empty — migrate from localStorage
          const localSettings = storage.getSettings();
          const hasLocalData = localSettings.telegram?.botToken || localSettings.dingtalk?.webhook;
          if (hasLocalData) {
            // Save local settings to backend (will be encrypted)
            await api.settings.save({
              telegramBotToken: localSettings.telegram?.botToken || null,
              telegramChatId: localSettings.telegram?.chatId || null,
              dingtalkWebhook: localSettings.dingtalk?.webhook || null,
              dingtalkSecret: localSettings.dingtalk?.secret || null
            });
            // Update state with local values
            setSettings(localSettings);
          }
        }
      } catch (err) {
        console.error('Failed to load/migrate settings:', err);
      } finally {
        setLoadingSettings(false);
      }
    };
    loadSettings();
  }, []);

  const saveSettings = async () => {
    // Save integration settings to backend (encrypted)
    try {
      await api.settings.save({
        telegramBotToken: settings.telegram.botToken || null,
        telegramChatId: settings.telegram.chatId || null,
        dingtalkWebhook: settings.dingtalk.webhook || null,
        dingtalkSecret: settings.dingtalk.secret || null
      });
    } catch (err) {
      console.error('Failed to save integration settings:', err);
      setError('Failed to save integration settings');
      return;
    }
    // Save non-sensitive settings locally
    storage.saveSettings(settings);
    setSaved(true);
    toast.success('Settings saved');
    setTimeout(() => setSaved(false), 2000);
  };

  const sendTelegramTest = async () => {
    setTelegramTestStatus('sending');
    try {
      const request: IntegrationRequest = {
        message: '🧪 Test message from NoteKeeper',
        subject: 'Test',
        botToken: settings.telegram.botToken,
        chatId: settings.telegram.chatId
      };
      const response: IntegrationResponse = await api.integrations.sendToTelegram(request);
      setTelegramTestStatus(response.success ? 'success' : 'error');
    } catch (err) {
      setTelegramTestStatus('error');
      setError((err as any)?.message || 'Telegram test failed');
    }
    setTimeout(() => setTelegramTestStatus('idle'), 3000);
  };

  const sendDingtalkTest = async () => {
    setDingtalkTestStatus('sending');
    try {
      const request: IntegrationRequest = {
        message: '🧪 Test message from NoteKeeper',
        subject: 'Test',
        webhook: settings.dingtalk.webhook,
        secret: settings.dingtalk.secret
      };
      const response: IntegrationResponse = await api.integrations.sendToDingtalk(request);
      setDingtalkTestStatus(response.success ? 'success' : 'error');
    } catch (err) {
      setDingtalkTestStatus('error');
      setError((err as any)?.message || 'DingTalk test failed');
    }
    setTimeout(() => setDingtalkTestStatus('idle'), 3000);
  };

  const exportBackup = async () => {
    setBackupStatus('exporting');
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/backup/export', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('Export failed');
      }

      // Download file
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `backup_${new Date().toISOString().slice(0,19).replace(/:/g,'-')}.json`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      setBackupStatus('success');
      setBackupMessage('Backup exported successfully!');
    } catch (err: any) {
      setBackupStatus('error');
      setBackupMessage(err.message || 'Export failed');
    }
    setTimeout(() => {
      setBackupStatus('idle');
      setBackupMessage('');
    }, 3000);
  };

  const importBackup = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setBackupStatus('importing');
    try {
      const token = localStorage.getItem('token');
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch('/api/v1/backup/import', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Import failed');
      }

      setBackupStatus('success');
      setBackupMessage('Backup imported successfully!');
      event.target.value = ''; // Reset file input
    } catch (err: any) {
      setBackupStatus('error');
      setBackupMessage(err.message || 'Import failed');
    }
    setTimeout(() => {
      setBackupStatus('idle');
      setBackupMessage('');
    }, 3000);
  };

  const loadBackupSettings = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/backup/settings', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to load backup settings');
      }

      const data = await response.json();
      setBackupSettings({
        enabled: data.enabled,
        cron: data.cron,
        retentionDays: data.retentionDays
      });
    } catch (err: any) {
      setError((err as any)?.message || 'Failed to load backup settings');
    }
  };

  const saveBackupSettings = async () => {
    setSettingsSaved(false);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/v1/backup/settings', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(backupSettings)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to save settings');
      }

      setSettingsSaved(true);
      setTimeout(() => setSettingsSaved(false), 2000);
    } catch (err: any) {
      setError((err as any)?.message || 'Failed to save backup settings');
    }
  };

  // Load backup settings when backup tab is opened
  useEffect(() => {
    if (activeTab === 'backup') {
      loadBackupSettings();
    }
  }, [activeTab]);

  return (
    <PageShell error={error} onDismissError={() => setError(null)} className="overflow-hidden">
      <Header
        title="Settings"
        actions={
          (activeTab === 'integrations' || activeTab === 'shortcuts') && (
            <button
              onClick={saveSettings}
              className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
            >
              <i className="fas fa-save mr-2"></i>
              {saved ? 'Saved!' : 'Save Settings'}
            </button>
          )
        }
      />

      <div className="flex-1 overflow-auto p-8 min-h-0">
        <div className="max-w-5xl mx-auto">
          <SettingsTabBar activeTab={activeTab} onTabChange={setActiveTab} />

          {activeTab === 'integrations' && (
            <div className="space-y-8">
              <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h3 className="text-xl font-bold text-text flex items-center gap-2">
                      <i className="fab fa-telegram text-blue-500"></i>
                      Telegram Integration
                    </h3>
                    <p className="text-sm text-text-secondary mt-1">
                      Connect your Telegram bot to receive notifications
                    </p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.telegram.enabled}
                      onChange={(e) => setSettings({
                        ...settings,
                        telegram: { ...settings.telegram, enabled: e.target.checked }
                      })}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                  </label>
                </div>

                {settings.telegram.enabled && (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-text mb-2">
                        Bot Token
                      </label>
                      <div className="relative">
                        <input
                          type={showTelegramToken ? 'text' : 'password'}
                          value={settings.telegram.botToken}
                          onChange={(e) => setSettings({
                            ...settings,
                            telegram: { ...settings.telegram, botToken: e.target.value }
                          })}
                          className="w-full px-4 py-2 pr-10 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                          placeholder="Enter your Telegram bot token"
                        />
                        <button
                          type="button"
                          onClick={() => setShowTelegramToken(!showTelegramToken)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary hover:text-text"
                          tabIndex={-1}
                        >
                          <i className={`fas ${showTelegramToken ? 'fa-eye-slash' : 'fa-eye'}`}></i>
                        </button>
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text mb-2">
                        Chat ID
                      </label>
                      <div className="relative">
                        <input
                          type={showTelegramChatId ? 'text' : 'password'}
                          value={settings.telegram.chatId}
                          onChange={(e) => setSettings({
                            ...settings,
                            telegram: { ...settings.telegram, chatId: e.target.value }
                          })}
                          className="w-full px-4 py-2 pr-10 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                          placeholder="Enter your chat ID"
                        />
                        <button
                          type="button"
                          onClick={() => setShowTelegramChatId(!showTelegramChatId)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary hover:text-text"
                          tabIndex={-1}
                        >
                          <i className={`fas ${showTelegramChatId ? 'fa-eye-slash' : 'fa-eye'}`}></i>
                        </button>
                      </div>
                    </div>
                    <div className="flex gap-2 pt-2">
                      <button
                        onClick={sendTelegramTest}
                        disabled={telegramTestStatus === 'sending'}
                        className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                          telegramTestStatus === 'success'
                            ? 'bg-green-500 text-white'
                            : telegramTestStatus === 'error'
                            ? 'bg-red-500 text-white'
                            : 'bg-primary text-white hover:bg-primary/90'
                        } disabled:opacity-50`}
                      >
                        <i className={`fas ${
                          telegramTestStatus === 'sending' ? 'fa-spinner fa-spin' :
                          telegramTestStatus === 'success' ? 'fa-check' :
                          telegramTestStatus === 'error' ? 'fa-times' : 'fa-paper-plane'
                        }`}></i>
                        {telegramTestStatus === 'sending' ? 'Sending...' :
                         telegramTestStatus === 'success' ? 'Sent!' :
                         telegramTestStatus === 'error' ? 'Failed' : 'Send Test Message'}
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h3 className="text-xl font-bold text-text flex items-center gap-2">
                      <i className="fas fa-comment text-blue-600"></i>
                      DingTalk Integration
                    </h3>
                    <p className="text-sm text-text-secondary mt-1">
                      Connect your DingTalk webhook to receive notifications
                    </p>
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={settings.dingtalk.enabled}
                      onChange={(e) => setSettings({
                        ...settings,
                        dingtalk: { ...settings.dingtalk, enabled: e.target.checked }
                      })}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                  </label>
                </div>

                {settings.dingtalk.enabled && (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-text mb-2">
                        Webhook URL
                      </label>
                      <div className="relative">
                        <input
                          type={showDingtalkWebhook ? 'text' : 'password'}
                          value={settings.dingtalk.webhook}
                          onChange={(e) => setSettings({
                            ...settings,
                            dingtalk: { ...settings.dingtalk, webhook: e.target.value }
                          })}
                          className="w-full px-4 py-2 pr-10 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                          placeholder="Enter your DingTalk webhook URL"
                        />
                        <button
                          type="button"
                          onClick={() => setShowDingtalkWebhook(!showDingtalkWebhook)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary hover:text-text"
                          tabIndex={-1}
                        >
                          <i className={`fas ${showDingtalkWebhook ? 'fa-eye-slash' : 'fa-eye'}`}></i>
                        </button>
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text mb-2">
                        Secret
                      </label>
                      <div className="relative">
                        <input
                          type={showDingtalkSecret ? 'text' : 'password'}
                          value={settings.dingtalk.secret}
                          onChange={(e) => setSettings({
                            ...settings,
                            dingtalk: { ...settings.dingtalk, secret: e.target.value }
                          })}
                          className="w-full px-4 py-2 pr-10 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                          placeholder="Enter your DingTalk secret"
                        />
                        <button
                          type="button"
                          onClick={() => setShowDingtalkSecret(!showDingtalkSecret)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary hover:text-text"
                          tabIndex={-1}
                        >
                          <i className={`fas ${showDingtalkSecret ? 'fa-eye-slash' : 'fa-eye'}`}></i>
                        </button>
                      </div>
                    </div>
                    <div className="flex gap-2 pt-2">
                      <button
                        onClick={sendDingtalkTest}
                        disabled={dingtalkTestStatus === 'sending'}
                        className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                          dingtalkTestStatus === 'success'
                            ? 'bg-green-500 text-white'
                            : dingtalkTestStatus === 'error'
                            ? 'bg-red-500 text-white'
                            : 'bg-primary text-white hover:bg-primary/90'
                        } disabled:opacity-50`}
                      >
                        <i className={`fas ${
                          dingtalkTestStatus === 'sending' ? 'fa-spinner fa-spin' :
                          dingtalkTestStatus === 'success' ? 'fa-check' :
                          dingtalkTestStatus === 'error' ? 'fa-times' : 'fa-paper-plane'
                        }`}></i>
                        {dingtalkTestStatus === 'sending' ? 'Sending...' :
                         dingtalkTestStatus === 'success' ? 'Sent!' :
                         dingtalkTestStatus === 'error' ? 'Failed' : 'Send Test Message'}
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <div className="bg-gradient-to-r from-primary to-secondary text-white rounded-xl p-6">
                <h3 className="text-lg font-bold mb-2">About NoteKeeper</h3>
                <p className="text-sm opacity-90">
                  Open-source Evernote alternative with powerful features for notes and todos management.
                </p>
                <p className="text-xs opacity-75 mt-4">Version 1.0.0</p>
              </div>
            </div>
          )}

          {activeTab === 'shortcuts' && (
            <div className="space-y-6">
              <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
                <h3 className="text-xl font-bold text-text mb-4">
                  <i className="fas fa-keyboard mr-2"></i>
                  Keyboard Shortcuts
                </h3>
                <p className="text-sm text-text-secondary mb-6">
                  Customize keyboard shortcuts for quick actions
                </p>

                <div className="space-y-4">
                  <div className="flex items-center justify-between p-4 bg-background rounded-lg">
                    <div>
                      <p className="font-medium text-text">New Note</p>
                      <p className="text-sm text-text-secondary">Create a new note quickly</p>
                    </div>
                    <input
                      type="text"
                      value={settings.shortcuts.newNote}
                      onChange={(e) => setSettings({
                        ...settings,
                        shortcuts: { ...settings.shortcuts, newNote: e.target.value }
                      })}
                      className="px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text w-32 text-center font-mono"
                      placeholder="ctrl+n"
                    />
                  </div>

                  <div className="flex items-center justify-between p-4 bg-background rounded-lg">
                    <div>
                      <p className="font-medium text-text">New Todo</p>
                      <p className="text-sm text-text-secondary">Create a new todo quickly</p>
                    </div>
                    <input
                      type="text"
                      value={settings.shortcuts.newTodo}
                      onChange={(e) => setSettings({
                        ...settings,
                        shortcuts: { ...settings.shortcuts, newTodo: e.target.value }
                      })}
                      className="px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text w-32 text-center font-mono"
                      placeholder="ctrl+t"
                    />
                  </div>

                  <div className="flex items-center justify-between p-4 bg-background rounded-lg">
                    <div>
                      <p className="font-medium text-text">Search</p>
                      <p className="text-sm text-text-secondary">Open search dialog</p>
                    </div>
                    <input
                      type="text"
                      value={settings.shortcuts.search}
                      onChange={(e) => setSettings({
                        ...settings,
                        shortcuts: { ...settings.shortcuts, search: e.target.value }
                      })}
                      className="px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text w-32 text-center font-mono"
                      placeholder="ctrl+k"
                    />
                  </div>

                  <div className="flex items-center justify-between p-4 bg-background rounded-lg">
                    <div>
                      <p className="font-medium text-text">Toggle Sidebar</p>
                      <p className="text-sm text-text-secondary">Show/hide sidebar</p>
                    </div>
                    <input
                      type="text"
                      value={settings.shortcuts.toggleSidebar}
                      onChange={(e) => setSettings({
                        ...settings,
                        shortcuts: { ...settings.shortcuts, toggleSidebar: e.target.value }
                      })}
                      className="px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text w-32 text-center font-mono"
                      placeholder="ctrl+b"
                    />
                  </div>
                </div>

                <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-sm text-blue-900">
                    <i className="fas fa-info-circle mr-2"></i>
                    Use format: <code className="bg-blue-100 px-2 py-1 rounded">ctrl+key</code>, <code className="bg-blue-100 px-2 py-1 rounded">shift+key</code>, or <code className="bg-blue-100 px-2 py-1 rounded">alt+key</code>
                  </p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'api' && <ApiTab />}

          {activeTab === 'backup' && (
            <div className="space-y-8">
              <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
                <div className="mb-6">
                  <h3 className="text-xl font-bold text-text flex items-center gap-2">
                    <i className="fas fa-download text-green-500"></i>
                    Export Backup
                  </h3>
                  <p className="text-sm text-text-secondary mt-1">
                    Download all your data as a JSON file
                  </p>
                </div>

                <div className="flex items-center gap-4">
                  <button
                    onClick={exportBackup}
                    disabled={backupStatus === 'exporting'}
                    className={`px-6 py-3 rounded-lg transition-colors flex items-center gap-2 ${
                      backupStatus === 'success'
                        ? 'bg-green-500 text-white'
                        : backupStatus === 'error'
                        ? 'bg-red-500 text-white'
                        : 'bg-primary text-white hover:bg-primary/90'
                    } disabled:opacity-50`}
                  >
                    <i className={`fas ${
                      backupStatus === 'exporting' ? 'fa-spinner fa-spin' :
                      backupStatus === 'success' ? 'fa-check' :
                      backupStatus === 'error' ? 'fa-times' : 'fa-download'
                    }`}></i>
                    {backupStatus === 'exporting' ? 'Exporting...' :
                     backupStatus === 'success' ? 'Exported!' :
                     backupStatus === 'error' ? 'Failed' : 'Export Data'}
                  </button>
                  {backupMessage && (
                    <span className={`text-sm ${
                      backupStatus === 'error' ? 'text-red-500' : 'text-green-500'
                    }`}>
                      {backupMessage}
                    </span>
                  )}
                </div>
              </div>

              <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
                <div className="mb-6">
                  <h3 className="text-xl font-bold text-text flex items-center gap-2">
                    <i className="fas fa-upload text-blue-500"></i>
                    Import Backup
                  </h3>
                  <p className="text-sm text-text-secondary mt-1">
                    Restore data from a previously exported backup file
                  </p>
                </div>

                <div className="flex items-center gap-4">
                  <label className={`px-6 py-3 rounded-lg transition-colors flex items-center gap-2 cursor-pointer ${
                    backupStatus === 'importing'
                      ? 'bg-gray-400 text-white'
                      : backupStatus === 'success'
                      ? 'bg-green-500 text-white'
                      : backupStatus === 'error'
                      ? 'bg-red-500 text-white'
                      : 'bg-blue-500 text-white hover:bg-blue-600'
                  } disabled:opacity-50`}>
                    <i className={`fas ${
                      backupStatus === 'importing' ? 'fa-spinner fa-spin' :
                      backupStatus === 'success' ? 'fa-check' :
                      backupStatus === 'error' ? 'fa-times' : 'fa-upload'
                    }`}></i>
                    {backupStatus === 'importing' ? 'Importing...' :
                     backupStatus === 'success' ? 'Imported!' :
                     backupStatus === 'error' ? 'Failed' : 'Import File'}
                    <input
                      type="file"
                      accept=".json"
                      onChange={importBackup}
                      disabled={backupStatus === 'importing'}
                      className="hidden"
                    />
                  </label>
                  {backupMessage && (
                    <span className={`text-sm ${
                      backupStatus === 'error' ? 'text-red-500' : 'text-green-500'
                    }`}>
                      {backupMessage}
                    </span>
                  )}
                </div>

                <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <p className="text-sm text-yellow-800">
                    <i className="fas fa-exclamation-triangle mr-2"></i>
                    <strong>Warning:</strong> Importing a backup will overwrite existing data. Make sure to export your current data first.
                  </p>
                </div>
              </div>

              <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h3 className="text-xl font-bold text-text flex items-center gap-2">
                      <i className="fas fa-clock text-purple-500"></i>
                      Automatic Backup Settings
                    </h3>
                    <p className="text-sm text-text-secondary mt-1">
                      Configure scheduled automatic backups
                    </p>
                  </div>
                  <button
                    onClick={saveBackupSettings}
                    className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
                  >
                    <i className="fas fa-save mr-2"></i>
                    {settingsSaved ? 'Saved!' : 'Save Settings'}
                  </button>
                </div>

                <div className="space-y-4">
                  <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                    <div>
                      <label className="text-sm font-medium text-text">Enable Automatic Backup</label>
                      <p className="text-xs text-text-secondary mt-1">
                        Automatically create backups on schedule
                      </p>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={backupSettings.enabled}
                        onChange={(e) => setBackupSettings({
                          ...backupSettings,
                          enabled: e.target.checked
                        })}
                        className="sr-only peer"
                      />
                      <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
                    </label>
                  </div>

                  <div className="p-4 bg-gray-50 rounded-lg">
                    <label className="block text-sm font-medium text-text mb-2">
                      Cron Schedule
                    </label>
                    <input
                      type="text"
                      value={backupSettings.cron}
                      onChange={(e) => setBackupSettings({
                        ...backupSettings,
                        cron: e.target.value
                      })}
                      className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text font-mono text-sm"
                      placeholder="0 0 2 * * *"
                    />
                    <p className="text-xs text-text-secondary mt-2">
                      <strong>Examples:</strong>
                      <br />
                      <code className="bg-white px-2 py-1 rounded">0 0 2 * * *</code> - Daily at 2 AM
                      <br />
                      <code className="bg-white px-2 py-1 rounded">0 0 * * * *</code> - Every hour
                      <br />
                      <code className="bg-white px-2 py-1 rounded">0 30 8 * * MON-FRI</code> - Weekdays at 8:30 AM
                    </p>
                  </div>

                  <div className="p-4 bg-gray-50 rounded-lg">
                    <label className="block text-sm font-medium text-text mb-2">
                      Retention Period (days)
                    </label>
                    <input
                      type="number"
                      value={backupSettings.retentionDays}
                      onChange={(e) => setBackupSettings({
                        ...backupSettings,
                        retentionDays: parseInt(e.target.value) || 30
                      })}
                      className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                      min="1"
                      max="365"
                    />
                    <p className="text-xs text-text-secondary mt-2">
                      Backups older than this will be automatically deleted
                    </p>
                  </div>

                  <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                    <p className="text-sm text-blue-800">
                      <i className="fas fa-info-circle mr-2"></i>
                      <strong>Note:</strong> Backups are stored in: <code className="bg-white px-2 py-1 rounded">./backups/</code>
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </PageShell>
  );
};
