/**
 * @module TodoEditor
 * @category Pages
 * @description Todo editor page — edit title, description, tags, schedule, location, and attachments.
 */
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { MarkdownRenderer } from '../components/MarkdownRenderer';
import { ShareModal } from '../components/ShareModal';
import { TagInput } from '../components/TagInput';
import { Todo, Attachment, TodoInput } from '../types';
import { storage } from '../utils/storage';
import { IntegrationRequest, IntegrationResponse } from '../types';

const emptyTodo = (): Todo => ({
  id: 'new',
  title: '',
  description: '',
  tags: [],
  priority: 'medium',
  isFavorite: false,
  completed: false,
  isArchived: false,
  isDeleted: false,
  ownerId: '',
  sharedWith: '[]',
  createdAt: new Date(),
  updatedAt: new Date(),
  attachments: [],
  schedule: { repeat: 'none', endDate: undefined }
});

/** Todo editor page. Supports Markdown description, tags, due date, reminder, recurrence schedule, geolocation, Telegram/DingTalk send, and attachments. */
export const TodoEditor: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [todo, setTodo] = useState<Todo | null>(null);
  const todoRef = React.useRef<Todo | null>(null);
  const [isPreview, setIsPreview] = useState(false);
  const [telegramStatus, setTelegramStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');
  const [dingtalkStatus, setDingtalkStatus] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');
  const [showShareModal, setShowShareModal] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const settings = storage.getSettings();

  // Keep ref in sync with state so saveTodo/addTag always see latest todo
  todoRef.current = todo;

  useEffect(() => {
    if (id) {
      let cancelled = false;
      const load = async () => {
        try {
          const t = await api.todos.getById(id);
          if (cancelled) return;
          if (t && t.id) {
            setTodo(t);
          } else {
            setTodo(emptyTodo());
          }
        } catch (err: any) {
          if (!cancelled) setError(err?.message || 'Failed to load todo');
        }
      };
      load();
      return () => { cancelled = true; };
    } else {
      setTodo(emptyTodo());
    }
  }, [id]);

  const saveTodo = async () => {
    const current = todoRef.current;
    if (!current) return;

    try {
      // Upload new attachments (those with blob: URLs) to server
      const uploadedAttachments: any[] = [];
      if (current.attachments && current.attachments.length > 0) {
        for (const att of current.attachments) {
          if (att.url.startsWith('blob:')) {
            console.log('[TodoEditor] Uploading attachment:', att.name);
            const response = await fetch(att.url);
            const blob = await response.blob();
            const file = new File([blob], att.name, { type: att.type });
            const uploaded = await api.attachments.upload(file, current.id, 'todo');
            uploadedAttachments.push(uploaded);
          } else {
            uploadedAttachments.push(att);
          }
        }
      }

      // Validate dates are in the future and reminder <= due date
      const now = new Date();
      let dueDateObj: Date | null = null;
      if (current.dueDate) {
        dueDateObj = typeof current.dueDate === 'string' ? new Date(current.dueDate) : current.dueDate;
        if (dueDateObj < now) {
          setError('Due date must be in the future');
          return;
        }
      }
      if (current.reminder) {
        const reminderObj = typeof current.reminder === 'string' ? new Date(current.reminder) : current.reminder;
        if (reminderObj < now) {
          setError('Reminder must be in the future');
          return;
        }
        if (dueDateObj && reminderObj > dueDateObj) {
          setError('Reminder cannot be after due date');
          return;
        }
      }

      const input: TodoInput = {
        title: current.title,
        description: current.description,
        tags: current.tags ?? [],
        priority: current.priority,
        isFavorite: current.isFavorite,
        completed: current.completed,
        dueDate: current.dueDate ? (typeof current.dueDate === 'string' ? current.dueDate : current.dueDate.toISOString()) : undefined,
        reminder: current.reminder ? (typeof current.reminder === 'string' ? current.reminder : current.reminder.toISOString()) : undefined,
        location: current.location,
        schedule: current.schedule && current.schedule.repeat !== 'none' ? {
          repeat: current.schedule.repeat,
          endDate: current.schedule.endDate ? (typeof current.schedule.endDate === 'string' ? current.schedule.endDate : new Date(current.schedule.endDate).toISOString()) : undefined
        } : undefined,
        notificationChannels: current.notificationChannels || undefined,
        attachments: uploadedAttachments.map(att => ({
          id: att.id,
          name: att.name,
          size: att.size,
          type: att.type,
          url: att.url,
          uploadedAt: att.uploadedAt instanceof Date ? att.uploadedAt.toISOString() : att.uploadedAt
        })),
      };
      console.log('[TodoEditor] Saving todo:', JSON.stringify({ id: current.id, schedule: input.schedule }, null, 2));
      if (current.id === 'new') {
        await api.todos.create(input);
      } else {
        await api.todos.update(current.id, input);
      }
      navigate('/todos');
    } catch (err: any) {
      setError(err?.message || 'Failed to save todo');
    }
  };

  const sendToTelegram = async () => {
    setTelegramStatus('sending');
    try {
      const request: IntegrationRequest = {
        message: `📋 Todo: ${todo?.title}\n${todo?.description?.substring(0, 100) || ''}`,
        subject: 'Todo',
        botToken: settings.telegram.botToken,
        chatId: settings.telegram.chatId
      };
      const response: IntegrationResponse = await api.integrations.sendToTelegram(request);
      setTelegramStatus(response.success ? 'success' : 'error');
    } catch (err: any) {
      setTelegramStatus('error');
      setError(err?.message || 'Failed to send to Telegram');
    }
    setTimeout(() => setTelegramStatus('idle'), 3000);
  };

  const sendToDingtalk = async () => {
    setDingtalkStatus('sending');
    try {
      const request: IntegrationRequest = {
        message: `📋 Todo: ${todo?.title}\n${todo?.description?.substring(0, 100) || ''}`,
        subject: 'Todo',
        webhook: settings.dingtalk.webhook,
        secret: settings.dingtalk.secret
      };
      const response: IntegrationResponse = await api.integrations.sendToDingtalk(request);
      setDingtalkStatus(response.success ? 'success' : 'error');
    } catch (err: any) {
      setDingtalkStatus('error');
      setError(err?.message || 'Failed to send to DingTalk');
    }
    setTimeout(() => setDingtalkStatus('idle'), 3000);
  };

  const formatDateTimeLocal = (date: Date | string): string => {
    const d = typeof date === 'string' ? new Date(date) : new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };



  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || !todoRef.current) return;

    const newAttachments: Attachment[] = Array.from(files).map(file => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: URL.createObjectURL(file),
      uploadedAt: new Date()
    }));

    setTodo(prev => prev ? { ...prev, attachments: [...(prev.attachments || []), ...newAttachments] } : prev);
  };

  const removeAttachment = (id: string) => {
    setTodo(prev => prev ? { ...prev, attachments: (prev.attachments || []).filter(a => a.id !== id) } : prev);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (!todo) return (
    <div className="flex-1 flex flex-col items-center justify-center gap-4 p-8">
      {error ? (
        <div className="flex flex-col items-center gap-3 text-center">
          <i className="fas fa-circle-exclamation text-red-400 text-4xl"></i>
          <p className="text-red-600 font-medium">{error}</p>
          <button
            onClick={() => navigate('/todos')}
            className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
          >
            Back to Todos
          </button>
        </div>
      ) : (
        <div className="text-gray-400 flex items-center gap-2">
          <i className="fas fa-spinner fa-spin"></i>
          Loading...
        </div>
      )}
    </div>
  );

  return (
    <div className="flex-1 flex flex-col bg-white min-h-0 overflow-hidden">
      {error && (
        <div className="flex items-center gap-3 px-4 py-3 bg-red-50 border-b border-red-200 text-red-700 text-sm">
          <i className="fas fa-circle-exclamation shrink-0"></i>
          <span className="flex-1">{error}</span>
          <button onClick={() => setError(null)} className="shrink-0 hover:text-red-900">
            <i className="fas fa-times"></i>
          </button>
        </div>
      )}
      <div className="border-b border-gray-200 px-4 sm:px-8 py-3 sm:py-4 flex items-center justify-between gap-2">
        <button
          onClick={() => navigate('/todos')}
          className="text-gray-600 hover:text-gray-800 shrink-0 flex items-center gap-1"
        >
          <i className="fas fa-arrow-left"></i>
          <span className="hidden sm:inline ml-1">Back</span>
        </button>
        <div className="flex items-center gap-1 sm:gap-3 overflow-x-auto">
          <button
            onClick={() => setIsPreview(!isPreview)}
            className={`p-2 sm:px-4 sm:py-2 rounded-lg transition-colors shrink-0 ${
              isPreview ? 'bg-primary text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <i className={`fas ${isPreview ? 'fa-edit' : 'fa-eye'} sm:mr-2`}></i>
            <span className="hidden sm:inline">{isPreview ? 'Edit' : 'Preview'}</span>
          </button>
          <div className="flex items-center gap-1 sm:gap-2 border-r border-gray-300 pr-1 sm:pr-3">
            <button
              onClick={sendToTelegram}
              disabled={telegramStatus === 'sending'}
              className={`p-2 rounded-lg transition-colors shrink-0 ${
                telegramStatus === 'success'
                  ? 'bg-green-500 text-white'
                  : telegramStatus === 'error'
                  ? 'bg-red-500 text-white'
                  : 'hover:bg-gray-100 text-blue-500'
              } disabled:opacity-50`}
              title="Send to Telegram"
            >
              <i className={`fab fa-telegram ${telegramStatus === 'sending' ? 'fa-spin' : ''}`}></i>
            </button>
            <button
              onClick={sendToDingtalk}
              disabled={dingtalkStatus === 'sending'}
              className={`p-2 rounded-lg transition-colors shrink-0 ${
                dingtalkStatus === 'success'
                  ? 'bg-green-500 text-white'
                  : dingtalkStatus === 'error'
                  ? 'bg-red-500 text-white'
                  : 'hover:bg-gray-100 text-blue-600'
              } disabled:opacity-50`}
              title="Send to DingTalk"
            >
              <i className={`fas fa-comment ${dingtalkStatus === 'sending' ? 'fa-spin' : ''}`}></i>
            </button>
          </div>
          <button
            onClick={() => setTodo(prev => prev ? { ...prev, isFavorite: !prev.isFavorite } : prev)}
            className="p-2 hover:bg-gray-100 rounded-lg shrink-0"
          >
            <i className={`fas fa-star ${todo.isFavorite ? 'text-yellow-500' : 'text-gray-400'}`}></i>
          </button>
          <button
            onClick={() => setShowShareModal(true)}
            className="p-2 hover:bg-gray-100 rounded-lg shrink-0"
            title="Share"
          >
            <i className="fas fa-share-alt text-gray-400"></i>
          </button>
          <select
            value={todo.priority}
            onChange={(e) => { const v = e.target.value as any; setTodo(prev => prev ? { ...prev, priority: v } : prev); }}
            className="px-2 py-2 border border-gray-300 rounded-lg text-sm shrink-0"
          >
            <option value="low">Low</option>
            <option value="medium">Med</option>
            <option value="high">High</option>
          </select>
          <button
            onClick={saveTodo}
            className="p-2 sm:px-6 sm:py-2 bg-primary text-white rounded-lg hover:bg-primary/90 shrink-0"
            title="Save"
          >
            <i className="fas fa-save sm:mr-2"></i>
            <span className="hidden sm:inline">Save</span>
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-4 sm:p-8 max-w-4xl mx-auto w-full min-h-0">
        <input
          type="text"
          value={todo.title}
          onChange={(e) => { const v = e.target.value; setTodo(prev => prev ? { ...prev, title: v } : prev); }}
          className="text-2xl sm:text-3xl font-bold mb-6 w-full border-none outline-none"
          placeholder="Todo Title"
        />

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description (Markdown supported)
            </label>
            {isPreview ? (
              <div className="border border-gray-300 rounded-lg p-4 bg-white min-h-32">
                <MarkdownRenderer content={todo.description} />
              </div>
            ) : (
              <textarea
                value={todo.description}
                onChange={(e) => { const v = e.target.value; setTodo(prev => prev ? { ...prev, description: v } : prev); }}
                className="w-full h-32 p-4 border border-gray-300 rounded-lg resize-none focus:outline-none focus:border-primary font-mono"
                placeholder="Add description... (Markdown supported)"
              />
            )}
          </div>

          <TagInput
            tags={todo.tags ?? []}
            onChange={(newTags) => setTodo(prev => prev ? { ...prev, tags: newTags } : prev)}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Due Date</label>
              <input
                type="datetime-local"
                value={todo.dueDate ? formatDateTimeLocal(todo.dueDate) : ''}
                onChange={(e) => { const v = e.target.value ? new Date(e.target.value) : undefined; setTodo(prev => prev ? { ...prev, dueDate: v } : prev); }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Reminder</label>
              <input
                type="datetime-local"
                value={todo.reminder ? formatDateTimeLocal(todo.reminder) : ''}
                onChange={(e) => { const v = e.target.value ? new Date(e.target.value) : undefined; setTodo(prev => prev ? { ...prev, reminder: v } : prev); }}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              <i className="fas fa-bell mr-2"></i>
              Notification Channels
            </label>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg cursor-pointer hover:bg-gray-50">
                <input
                  type="checkbox"
                  checked={todo.notificationChannels?.includes('telegram') || false}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setTodo(prev => {
                      if (!prev) return prev;
                      const channels = prev.notificationChannels?.split(',').filter(c => c) || [];
                      if (checked) {
                        if (!channels.includes('telegram')) channels.push('telegram');
                      } else {
                        const idx = channels.indexOf('telegram');
                        if (idx > -1) channels.splice(idx, 1);
                      }
                      return { ...prev, notificationChannels: channels.join(',') };
                    });
                  }}
                  className="text-primary focus:ring-primary"
                />
                <i className="fab fa-telegram text-blue-500"></i>
                <span>Telegram</span>
              </label>
              <label className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg cursor-pointer hover:bg-gray-50">
                <input
                  type="checkbox"
                  checked={todo.notificationChannels?.includes('dingtalk') || false}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setTodo(prev => {
                      if (!prev) return prev;
                      const channels = prev.notificationChannels?.split(',').filter(c => c) || [];
                      if (checked) {
                        if (!channels.includes('dingtalk')) channels.push('dingtalk');
                      } else {
                        const idx = channels.indexOf('dingtalk');
                        if (idx > -1) channels.splice(idx, 1);
                      }
                      return { ...prev, notificationChannels: channels.join(',') };
                    });
                  }}
                  className="text-primary focus:ring-primary"
                />
                <i className="fas fa-comment text-blue-600"></i>
                <span>DingTalk</span>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Schedule</label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <select
                value={todo.schedule?.repeat || 'none'}
                onChange={(e) => { const v = e.target.value as any; setTodo(prev => prev ? { ...prev, schedule: { ...prev.schedule, repeat: v } } : prev); }}
                className="px-4 py-2 border border-gray-300 rounded-lg"
              >
                <option value="none">No Repeat</option>
                <option value="daily">Daily</option>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
              </select>
              {todo.schedule?.repeat !== 'none' && (
                <div>
                  <label className="block text-xs text-gray-500 mb-1">Repeat until (optional)</label>
                  <input
                    type="date"
                    value={todo.schedule?.endDate ? (typeof todo.schedule.endDate === 'string' ? todo.schedule.endDate.slice(0, 10) : new Date(todo.schedule.endDate).toISOString().slice(0, 10)) : ''}
                    onChange={(e) => { const v = e.target.value || undefined; setTodo(prev => prev ? { ...prev, schedule: { ...prev.schedule!, endDate: v } } : prev); }}
                    className="px-4 py-2 border border-gray-300 rounded-lg w-full"
                    placeholder="End Date"
                  />
                </div>
              )}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Location</label>
            <input
              type="text"
              value={todo.location?.address || ''}
              onChange={(e) => { const v = e.target.value; setTodo(prev => prev ? { ...prev, location: v ? { lat: 0, lng: 0, address: v } : undefined } : prev); }}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              placeholder="Add location..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              <i className="fas fa-paperclip mr-2"></i>
              Attachments
            </label>
            <div className="space-y-3">
              {todo.attachments?.map(attachment => (
                <div key={attachment.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-200">
                  <div className="flex items-center gap-3">
                    <i className="fas fa-file text-gray-400 text-xl"></i>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{attachment.name}</p>
                      <p className="text-xs text-gray-500">{formatFileSize(attachment.size)}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <a
                      href={attachment.url}
                      download={attachment.name}
                      className="p-2 text-primary hover:bg-primary/10 rounded-lg transition-colors"
                    >
                      <i className="fas fa-download"></i>
                    </a>
                    <button
                      onClick={() => removeAttachment(attachment.id)}
                      className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                    >
                      <i className="fas fa-trash"></i>
                    </button>
                  </div>
                </div>
              ))}
              <label className="flex items-center justify-center gap-2 p-4 border-2 border-dashed border-gray-300 rounded-lg hover:border-primary hover:bg-primary/5 transition-colors cursor-pointer">
                <i className="fas fa-cloud-upload-alt text-gray-400"></i>
                <span className="text-sm text-gray-600">Upload files</span>
                <input
                  type="file"
                  multiple
                  onChange={handleFileUpload}
                  className="hidden"
                />
              </label>
            </div>
          </div>
        </div>
      </div>

      <ShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
        resourceId={todo.id}
        resourceType="todo"
        ownerId={todo.ownerId}
        sharedWith={todo.sharedWith}
        onShareSuccess={() => {
          if (id) {
            api.todos.getById(id).then(setTodo);
          }
        }}
      />
    </div>
  );
};
