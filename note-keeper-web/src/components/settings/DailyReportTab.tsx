import React, { useState } from 'react';
import { Settings } from '../../types';
import { api } from '../../utils/api';

interface DailyReportTabProps {
  settings: Settings;
  onSettingsChange: (settings: Settings) => void;
}

const BODY_VARIABLES = [
  { name: '{date}', desc: 'Report date (e.g. "15 Jan 2025")' },
  { name: '{todo_count}', desc: 'Number of pending todos' },
  { name: '{todo_list}', desc: 'Rendered list of todo items' },
];

const ITEM_VARIABLES = [
  { name: '{title}', desc: 'Todo title' },
  { name: '{priority}', desc: 'Priority level (high/medium/low)' },
  { name: '{priority_icon}', desc: 'Emoji icon (🔴 🟡 🟢)' },
  { name: '{due_date}', desc: 'Due date or empty' },
  { name: '{tags}', desc: 'Tags with # prefix or empty' },
  { name: '{link}', desc: 'Direct link to the todo' },
];

const DEFAULT_BODY_TEMPLATE =
  'Daily Report — {date}\n\nYou have {todo_count} pending todo(s):\n\n{todo_list}';
const DEFAULT_ITEM_TEMPLATE =
  '{priority_icon} {title}{due_date}{tags}';

export const DailyReportTab: React.FC<DailyReportTabProps> = ({ settings, onSettingsChange }) => {
  const [previewText, setPreviewText] = useState<string | null>(null);
  const [previewStatus, setPreviewStatus] = useState<'idle' | 'loading' | 'done' | 'error'>('idle');
  const [testStatus, setTestStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');

  const dr = settings.dailyReport;

  const update = (patch: Partial<typeof dr>) => {
    onSettingsChange({
      ...settings,
      dailyReport: { ...dr, ...patch }
    });
  };

  const handlePreview = async () => {
    setPreviewStatus('loading');
    setPreviewText(null);
    try {
      const text = await api.dailyReport.preview();
      setPreviewText(text);
      setPreviewStatus('done');
    } catch (err: any) {
      setPreviewText(err?.message || 'Failed to generate preview');
      setPreviewStatus('error');
    }
  };

  const handleTest = async () => {
    setTestStatus('sending');
    try {
      await api.dailyReport.test();
      setTestStatus('success');
    } catch {
      setTestStatus('error');
    }
    setTimeout(() => setTestStatus('idle'), 3000);
  };

  const parseChannels = (channels: string) => ({
    telegram: channels.split(',').map(c => c.trim()).includes('telegram'),
    dingtalk: channels.split(',').map(c => c.trim()).includes('dingtalk'),
  });

  const toggleChannel = (channel: string, checked: boolean) => {
    const current = parseChannels(dr.channels);
    const updated = { ...current, [channel]: checked };
    const active = Object.entries(updated).filter(([, v]) => v).map(([k]) => k).join(',');
    update({ channels: active || 'telegram' });
  };

  const channelState = parseChannels(dr.channels);

  return (
    <div className="space-y-8">
      {/* Enable / Time */}
      <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="text-xl font-bold text-text flex items-center gap-2">
              <i className="fas fa-bell text-primary"></i>
              Daily Report
            </h3>
            <p className="text-sm text-text-secondary mt-1">
              Receive a daily summary of your uncompleted todos
            </p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={dr.enabled}
              onChange={(e) => update({ enabled: e.target.checked })}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-border peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary/20 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-background after:border-border after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
          </label>
        </div>

        {dr.enabled && (
          <div className="space-y-4">
            {/* Time picker */}
            <div>
              <label className="block text-sm font-medium text-text mb-2">
                Report Time
              </label>
              <input
                type="time"
                value={dr.time}
                onChange={(e) => update({ time: e.target.value })}
                className="w-full sm:w-auto px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text"
              />
              <p className="text-xs text-text-secondary mt-1">
                Time in your local timezone
              </p>
            </div>

            {/* Channel checkboxes */}
            <div>
              <label className="block text-sm font-medium text-text mb-2">
                Notification Channels
              </label>
              <div className="flex flex-wrap gap-4">
                <label className="flex items-center gap-2 px-4 py-2 border border-border rounded-lg cursor-pointer hover:bg-hover">
                  <input
                    type="checkbox"
                    checked={channelState.telegram}
                    onChange={(e) => toggleChannel('telegram', e.target.checked)}
                    className="text-primary focus:ring-primary"
                  />
                  <i className="fab fa-telegram text-blue-500"></i>
                  <span className="text-text">Telegram</span>
                </label>
                <label className="flex items-center gap-2 px-4 py-2 border border-border rounded-lg cursor-pointer hover:bg-hover">
                  <input
                    type="checkbox"
                    checked={channelState.dingtalk}
                    onChange={(e) => toggleChannel('dingtalk', e.target.checked)}
                    className="text-primary focus:ring-primary"
                  />
                  <i className="fas fa-comment text-blue-600"></i>
                  <span className="text-text">DingTalk</span>
                </label>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Templates */}
      {dr.enabled && (
        <>
          <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
            <h3 className="text-lg font-bold text-text flex items-center gap-2 mb-4">
              <i className="fas fa-file-alt text-secondary"></i>
              Body Template
            </h3>
            <textarea
              value={dr.template || DEFAULT_BODY_TEMPLATE}
              onChange={(e) => update({ template: e.target.value })}
              rows={5}
              className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text font-mono text-sm resize-y"
              placeholder={DEFAULT_BODY_TEMPLATE}
            />
            <div className="mt-2">
              <p className="text-xs text-text-secondary mb-1">Available variables:</p>
              <div className="flex flex-wrap gap-2">
                {BODY_VARIABLES.map(v => (
                  <span key={v.name} className="inline-flex items-center gap-1 px-2 py-1 bg-hover rounded text-xs">
                    <code className="text-primary font-mono">{v.name}</code>
                    <span className="text-text-secondary">— {v.desc}</span>
                  </span>
                ))}
              </div>
            </div>
          </div>

          <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
            <h3 className="text-lg font-bold text-text flex items-center gap-2 mb-4">
              <i className="fas fa-list-ul text-secondary"></i>
              Item Template
            </h3>
            <textarea
              value={dr.itemTemplate || DEFAULT_ITEM_TEMPLATE}
              onChange={(e) => update({ itemTemplate: e.target.value })}
              rows={3}
              className="w-full px-4 py-2 border border-border rounded-lg focus:outline-none focus:border-primary bg-surface text-text font-mono text-sm resize-y"
              placeholder={DEFAULT_ITEM_TEMPLATE}
            />
            <div className="mt-2">
              <p className="text-xs text-text-secondary mb-1">Available variables:</p>
              <div className="flex flex-wrap gap-2">
                {ITEM_VARIABLES.map(v => (
                  <span key={v.name} className="inline-flex items-center gap-1 px-2 py-1 bg-hover rounded text-xs">
                    <code className="text-primary font-mono">{v.name}</code>
                    <span className="text-text-secondary">— {v.desc}</span>
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Preview & Test */}
          <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
            <h3 className="text-lg font-bold text-text flex items-center gap-2 mb-4">
              <i className="fas fa-eye text-primary"></i>
              Preview &amp; Test
            </h3>
            <div className="flex gap-2 mb-4">
              <button
                onClick={handlePreview}
                disabled={previewStatus === 'loading'}
                className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                  previewStatus === 'error'
                    ? 'bg-red-500 text-white'
                    : 'bg-primary text-white hover:bg-primary/90'
                } disabled:opacity-50`}
              >
                <i className={`fas ${previewStatus === 'loading' ? 'fa-spinner fa-spin' : 'fa-eye'}`}></i>
                {previewStatus === 'loading' ? 'Generating...' : 'Preview'}
              </button>
              <button
                onClick={handleTest}
                disabled={testStatus === 'sending'}
                className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                  testStatus === 'success'
                    ? 'bg-green-500 text-white'
                    : testStatus === 'error'
                    ? 'bg-red-500 text-white'
                    : 'bg-secondary text-white hover:bg-secondary/90'
                } disabled:opacity-50`}
              >
                <i className={`fas ${
                  testStatus === 'sending' ? 'fa-spinner fa-spin' :
                  testStatus === 'success' ? 'fa-check' :
                  testStatus === 'error' ? 'fa-times' : 'fa-paper-plane'
                }`}></i>
                {testStatus === 'sending' ? 'Sending...' :
                 testStatus === 'success' ? 'Sent!' :
                 testStatus === 'error' ? 'Failed' : 'Send Test'}
              </button>
            </div>

            {previewText !== null && (
              <div className={`p-4 rounded-lg border whitespace-pre-wrap font-mono text-sm ${
                previewStatus === 'error'
                  ? 'bg-red-500/10 border-red-500/30 text-red-500'
                  : 'bg-background border-border text-text'
              }`}>
                {previewText}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};
