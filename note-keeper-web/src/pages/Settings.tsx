import React, { useState, useEffect } from 'react';
import { Header } from '../components/Header';
import { storage } from '../utils/storage';
import { Settings as SettingsType } from '../types';
import { api } from '../utils/api';
import { IntegrationRequest, IntegrationResponse } from '../types';

export const Settings: React.FC = () => {
  const [settings, setSettings] = useState<SettingsType>(storage.getSettings());
  const [saved, setSaved] = useState(false);
  const [activeTab, setActiveTab] = useState<'integrations' | 'shortcuts' | 'api'>('integrations');
  const [telegramTestStatus, setTelegramTestStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');
  const [dingtalkTestStatus, setDingtalkTestStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');

  const saveSettings = () => {
    storage.saveSettings(settings);
    setSaved(true);
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
      console.error('Telegram test failed', err);
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
      console.error('DingTalk test failed', err);
    }
    setTimeout(() => setDingtalkTestStatus('idle'), 3000);
  };

  return (
    <div className="flex-1 flex flex-col bg-background">
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

      <div className="flex-1 overflow-auto p-8">
        <div className="max-w-5xl mx-auto">
          <div className="flex gap-4 mb-8 border-b border-border">
            <button
              onClick={() => setActiveTab('integrations')}
              className={`px-6 py-3 font-medium transition-colors ${
                activeTab === 'integrations'
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-text-secondary hover:text-text'
              }`}
            >
              <i className="fas fa-plug mr-2"></i>
              Integrations
            </button>
            <button
              onClick={() => setActiveTab('shortcuts')}
              className={`px-6 py-3 font-medium transition-colors ${
                activeTab === 'shortcuts'
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-text-secondary hover:text-text'
              }`}
            >
              <i className="fas fa-keyboard mr-2"></i>
              Shortcuts
            </button>
            <button
              onClick={() => setActiveTab('api')}
              className={`px-6 py-3 font-medium transition-colors ${
                activeTab === 'api'
                  ? 'text-primary border-b-2 border-primary'
                  : 'text-text-secondary hover:text-text'
              }`}
            >
              <i className="fas fa-code mr-2"></i>
              API Documentation
            </button>
          </div>

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
                      <input
                        type="text"
                        value={settings.telegram.botToken}
                        onChange={(e) => setSettings({
                          ...settings,
                          telegram: { ...settings.telegram, botToken: e.target.value }
                        })}
                        className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                        placeholder="Enter your Telegram bot token"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text mb-2">
                        Chat ID
                      </label>
                      <input
                        type="text"
                        value={settings.telegram.chatId}
                        onChange={(e) => setSettings({
                          ...settings,
                          telegram: { ...settings.telegram, chatId: e.target.value }
                        })}
                        className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                        placeholder="Enter your chat ID"
                      />
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
                      <input
                        type="text"
                        value={settings.dingtalk.webhook}
                        onChange={(e) => setSettings({
                          ...settings,
                          dingtalk: { ...settings.dingtalk, webhook: e.target.value }
                        })}
                        className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                        placeholder="Enter your DingTalk webhook URL"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-text mb-2">
                        Secret
                      </label>
                      <input
                        type="text"
                        value={settings.dingtalk.secret}
                        onChange={(e) => setSettings({
                          ...settings,
                          dingtalk: { ...settings.dingtalk, secret: e.target.value }
                        })}
                        className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
                        placeholder="Enter your DingTalk secret"
                      />
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

          {activeTab === 'api' && (
            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <div className="mb-6">
                <h3 className="text-2xl font-bold text-text mb-2">REST API Documentation</h3>
                <p className="text-text-secondary">
                  Complete OpenAPI 3.0 specification for NoteKeeper REST API
                </p>
              </div>

              <div className="bg-background rounded-lg p-6 mb-6">
                <h4 className="text-lg font-bold text-text mb-4">API Endpoints</h4>
                <div className="space-y-4">
                  <div className="border-l-4 border-primary pl-4">
                    <h5 className="font-bold text-text mb-2">Notes API</h5>
                    <div className="space-y-2 text-sm">
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-green-100 text-green-700 rounded font-mono text-xs">GET</span>
                        <code className="text-text-secondary">/api/v1/notes</code>
                        <span className="text-text-secondary">- Get all notes</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded font-mono text-xs">POST</span>
                        <code className="text-text-secondary">/api/v1/notes</code>
                        <span className="text-text-secondary">- Create note</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-green-100 text-green-700 rounded font-mono text-xs">GET</span>
                        <code className="text-text-secondary">/api/v1/notes/:id</code>
                        <span className="text-text-secondary">- Get note by ID</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-yellow-100 text-yellow-700 rounded font-mono text-xs">PUT</span>
                        <code className="text-text-secondary">/api/v1/notes/:id</code>
                        <span className="text-text-secondary">- Update note</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-red-100 text-red-700 rounded font-mono text-xs">DELETE</span>
                        <code className="text-text-secondary">/api/v1/notes/:id</code>
                        <span className="text-text-secondary">- Delete note</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded font-mono text-xs">POST</span>
                        <code className="text-text-secondary">/api/v1/notes/import</code>
                        <span className="text-text-secondary">- Import from file</span>
                      </div>
                    </div>
                  </div>

                  <div className="border-l-4 border-secondary pl-4">
                    <h5 className="font-bold text-text mb-2">Todos API</h5>
                    <div className="space-y-2 text-sm">
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-green-100 text-green-700 rounded font-mono text-xs">GET</span>
                        <code className="text-text-secondary">/api/v1/todos</code>
                        <span className="text-text-secondary">- Get all todos</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded font-mono text-xs">POST</span>
                        <code className="text-text-secondary">/api/v1/todos</code>
                        <span className="text-text-secondary">- Create todo</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-green-100 text-green-700 rounded font-mono text-xs">GET</span>
                        <code className="text-text-secondary">/api/v1/todos/:id</code>
                        <span className="text-text-secondary">- Get todo by ID</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-yellow-100 text-yellow-700 rounded font-mono text-xs">PUT</span>
                        <code className="text-text-secondary">/api/v1/todos/:id</code>
                        <span className="text-text-secondary">- Update todo</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-red-100 text-red-700 rounded font-mono text-xs">DELETE</span>
                        <code className="text-text-secondary">/api/v1/todos/:id</code>
                        <span className="text-text-secondary">- Delete todo</span>
                      </div>
                    </div>
                  </div>

                  <div className="border-l-4 border-purple-500 pl-4">
                    <h5 className="font-bold text-text mb-2">Integrations API</h5>
                    <div className="space-y-2 text-sm">
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded font-mono text-xs">POST</span>
                        <code className="text-text-secondary">/api/v1/integrations/telegram</code>
                        <span className="text-text-secondary">- Send to Telegram</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded font-mono text-xs">POST</span>
                        <code className="text-text-secondary">/api/v1/integrations/dingtalk</code>
                        <span className="text-text-secondary">- Send to DingTalk</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-background rounded-lg p-6">
                <h4 className="text-lg font-bold text-text mb-4">Full Swagger Specification</h4>
                <p className="text-text-secondary mb-4">
                  The complete OpenAPI 3.0 specification is available in the project root:
                </p>
                <div className="bg-surface border border-border rounded-lg p-4 flex items-center justify-between">
                  <code className="text-primary font-mono text-sm">swagger.yaml</code>
                  <a
                    href="/swagger.yaml"
                    download="swagger.yaml"
                    className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors text-sm"
                  >
                    <i className="fas fa-download mr-2"></i>
                    Download
                  </a>
                </div>
                <p className="text-text-secondary mt-4 text-sm">
                  You can view and test the API using:
                </p>
                <ul className="list-disc list-inside text-text-secondary text-sm mt-2 space-y-1">
                  <li>Swagger Editor: <a href="https://editor.swagger.io/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">editor.swagger.io</a></li>
                  <li>Swagger UI: <a href="https://swagger.io/tools/swagger-ui/" target="_blank" rel="noopener noreferrer" className="text-primary hover:underline">swagger.io/tools/swagger-ui</a></li>
                  <li>Postman: Import the swagger.yaml file</li>
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
