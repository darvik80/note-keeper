import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../utils/api';
import { MarkdownRenderer } from '../components/MarkdownRenderer';
import { Todo, Attachment, TodoInput } from '../types';

export const TodoEditor: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [todo, setTodo] = useState<Todo | null>(null);
  const [isPreview, setIsPreview] = useState(false);

  useEffect(() => {
    if (id) {
      const load = async () => {
        try {
          const t = await api.todos.getById(id);
          setTodo(t);
        } catch (err) {
          console.error('Failed to load todo', err);
        }
      };
      load();
    }
  }, [id]);

  const saveTodo = async () => {
    if (!todo) return;
    const input: TodoInput = {
      title: todo.title,
      description: todo.description,
      tags: todo.tags,
      priority: todo.priority,
      isFavorite: todo.isFavorite,
      completed: todo.completed,
      dueDate: todo.dueDate ? String(todo.dueDate) : undefined,
      reminder: todo.reminder ? String(todo.reminder) : undefined,
      location: todo.location,
      schedule: todo.schedule,
    };
    try {
      await api.todos.update(todo.id, input);
      navigate('/todos');
    } catch (err) {
      console.error('Failed to save todo', err);
    }
  };

  const addTag = (tag: string) => {
    if (!todo || !tag.trim() || todo.tags.includes(tag)) return;
    setTodo({ ...todo, tags: [...todo.tags, tag.trim()] });
  };

  const removeTag = (tag: string) => {
    if (!todo) return;
    setTodo({ ...todo, tags: todo.tags.filter(t => t !== tag) });
  };

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || !todo) return;

    const newAttachments: Attachment[] = Array.from(files).map(file => ({
      id: Date.now().toString() + Math.random(),
      name: file.name,
      size: file.size,
      type: file.type,
      url: URL.createObjectURL(file),
      uploadedAt: new Date()
    }));

    setTodo({ ...todo, attachments: [...(todo.attachments || []), ...newAttachments] });
  };

  const removeAttachment = (id: string) => {
    if (!todo) return;
    setTodo({ ...todo, attachments: (todo.attachments || []).filter(a => a.id !== id) });
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  if (!todo) return <div>Loading...</div>;

  return (
    <div className="flex-1 flex flex-col bg-white">
      <div className="border-b border-gray-200 px-8 py-4 flex items-center justify-between">
        <button
          onClick={() => navigate('/todos')}
          className="text-gray-600 hover:text-gray-800"
        >
          <i className="fas fa-arrow-left mr-2"></i>
          Back to Todos
        </button>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsPreview(!isPreview)}
            className={`px-4 py-2 rounded-lg transition-colors ${
              isPreview ? 'bg-primary text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            <i className={`fas ${isPreview ? 'fa-edit' : 'fa-eye'} mr-2`}></i>
            {isPreview ? 'Edit' : 'Preview'}
          </button>
          <button
            onClick={() => setTodo({ ...todo, isFavorite: !todo.isFavorite })}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            <i className={`fas fa-star ${todo.isFavorite ? 'text-yellow-500' : 'text-gray-400'}`}></i>
          </button>
          <select
            value={todo.priority}
            onChange={(e) => setTodo({ ...todo, priority: e.target.value as any })}
            className="px-3 py-2 border border-gray-300 rounded-lg"
          >
            <option value="low">Low Priority</option>
            <option value="medium">Medium Priority</option>
            <option value="high">High Priority</option>
          </select>
          <button
            onClick={saveTodo}
            className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
          >
            <i className="fas fa-save mr-2"></i>
            Save
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-8 max-w-4xl mx-auto w-full">
        <input
          type="text"
          value={todo.title}
          onChange={(e) => setTodo({ ...todo, title: e.target.value })}
          className="text-3xl font-bold mb-6 w-full border-none outline-none"
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
                onChange={(e) => setTodo({ ...todo, description: e.target.value })}
                className="w-full h-32 p-4 border border-gray-300 rounded-lg resize-none focus:outline-none focus:border-primary font-mono"
                placeholder="Add description... (Markdown supported)"
              />
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tags</label>
            <div className="flex flex-wrap gap-2 mb-3">
              {todo.tags.map(tag => (
                <span
                  key={tag}
                  className="bg-primary/10 text-primary px-3 py-1 rounded-full text-sm flex items-center gap-2"
                >
                  #{tag}
                  <button onClick={() => removeTag(tag)} className="hover:text-primary/70">
                    <i className="fas fa-times"></i>
                  </button>
                </span>
              ))}
            </div>
            <input
              type="text"
              placeholder="Add tag and press Enter"
              className="px-4 py-2 border border-gray-300 rounded-lg w-full"
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  addTag((e.target as HTMLInputElement).value);
                  (e.target as HTMLInputElement).value = '';
                }
              }}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Due Date</label>
              <input
                type="datetime-local"
                value={todo.dueDate ? new Date(todo.dueDate).toISOString().slice(0, 16) : ''}
                onChange={(e) => setTodo({ ...todo, dueDate: e.target.value ? new Date(e.target.value) : undefined })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Reminder</label>
              <input
                type="datetime-local"
                value={todo.reminder ? new Date(todo.reminder).toISOString().slice(0, 16) : ''}
                onChange={(e) => setTodo({ ...todo, reminder: e.target.value ? new Date(e.target.value) : undefined })}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Schedule</label>
            <div className="grid grid-cols-2 gap-4">
              <select
                value={todo.schedule?.repeat || 'none'}
                onChange={(e) => setTodo({
                  ...todo,
                  schedule: { ...todo.schedule, repeat: e.target.value as any }
                })}
                className="px-4 py-2 border border-gray-300 rounded-lg"
              >
                <option value="none">No Repeat</option>
                <option value="daily">Daily</option>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
              </select>
              {todo.schedule?.repeat !== 'none' && (
                <input
                  type="date"
                  value={todo.schedule?.endDate ? new Date(todo.schedule.endDate).toISOString().slice(0, 10) : ''}
                  onChange={(e) => setTodo({
                    ...todo,
                    schedule: { ...todo.schedule!, endDate: e.target.value ? new Date(e.target.value) : undefined }
                  })}
                  className="px-4 py-2 border border-gray-300 rounded-lg"
                  placeholder="End Date"
                />
              )}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Location</label>
            <input
              type="text"
              value={todo.location?.address || ''}
              onChange={(e) => setTodo({
                ...todo,
                location: e.target.value ? { lat: 0, lng: 0, address: e.target.value } : undefined
              })}
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
    </div>
  );
};
