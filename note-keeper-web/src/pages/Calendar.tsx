/**
 * @module Calendar
 * @category Pages
 * @description Calendar page — todos with due dates displayed in a monthly calendar grid.
 */
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Todo, Note } from '../types';
import { useWebSocket } from '../hooks/useWebSocket';

type CalendarItem = {
  id: string;
  title: string;
  type: 'todo' | 'note';
  completed?: boolean;
  priority: string;
  tags: string[];
  description?: string;
  dueDate?: Date | string;
  reminder?: Date | string;
  schedule?: { repeat: string; endDate?: string };
};

/** Calendar page displaying todos and notes by date in a monthly grid view. */
export const Calendar: React.FC = () => {
  const navigate = useNavigate();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [items, setItems] = useState<CalendarItem[]>([]);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [todos, notes] = await Promise.all([
        api.todos.getAll({ isArchived: false, isDeleted: false }),
        api.notes.getAll({ isArchived: false, isDeleted: false })
      ]);

      const todoItems: CalendarItem[] = todos
        .filter(t => t.dueDate || t.reminder)
        .map(t => ({
          id: t.id,
          title: t.title,
          type: 'todo' as const,
          completed: t.completed,
          priority: t.priority,
          tags: t.tags,
          description: t.description,
          dueDate: t.dueDate,
          reminder: t.reminder,
          schedule: t.schedule
        }));

      const noteItems: CalendarItem[] = notes
        .filter(n => n.reminder)
        .map(n => ({
          id: n.id,
          title: n.title,
          type: 'note' as const,
          priority: n.priority,
          tags: n.tags,
          description: n.content?.slice(0, 100),
          reminder: n.reminder
        }));

      setItems([...todoItems, ...noteItems]);
    } catch (err) {
      setError((err as any)?.message || 'Failed to load calendar data');
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  useWebSocket((event) => {
    if (event.type.startsWith('TODO_') || event.type.startsWith('NOTE_')) {
      load();
    }
  });

  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    return { daysInMonth, startingDayOfWeek };
  };

  const isSameDay = (a: Date, b: Date) =>
    a.getDate() === b.getDate() && a.getMonth() === b.getMonth() && a.getFullYear() === b.getFullYear();

  /**
   * Parse API date string (e.g. "2026-07-05T15:00:00Z") into local Date
   * by extracting date+time parts directly, ignoring timezone suffix.
   * This avoids UTC→local day shift that occurs with new Date("...Z").
   */
  const parseLocalDate = (dateStr: string | Date | undefined): Date | null => {
    if (!dateStr) return null;
    const str = typeof dateStr === 'string' ? dateStr : dateStr.toISOString();
    const match = str.match(/(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/);
    if (match) {
      return new Date(
        parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]),
        match[4] ? parseInt(match[4]) : 0,
        match[5] ? parseInt(match[5]) : 0
      );
    }
    const d = new Date(str);
    return isNaN(d.getTime()) ? null : d;
  };

  const getItemsForDate = (date: Date) => {
    // Normalize date to midnight for accurate day-level comparison
    const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());

    return items.filter(item => {
      // Check dueDate match
      if (item.dueDate) {
        const dueDate = parseLocalDate(item.dueDate);
        if (dueDate && isSameDay(dueDate, date)) {
          return true;
        }
      }
      // Check reminder match
      if (item.reminder) {
        const reminderDate = parseLocalDate(item.reminder);
        if (reminderDate && isSameDay(reminderDate, date)) {
          return true;
        }
      }
      // Check recurring schedule (todos only) — use dueDate or reminder as start
      if (item.type === 'todo' && item.schedule && item.schedule.repeat !== 'none' && (item.dueDate || item.reminder)) {
        const rawStart = parseLocalDate(item.dueDate || item.reminder);
        if (!rawStart) return false;
        const startDate = new Date(rawStart.getFullYear(), rawStart.getMonth(), rawStart.getDate());

        const endDateRaw = item.schedule.endDate;
        const endDate = endDateRaw
          ? (() => {
              // Parse date portion directly from ISO string to avoid UTC→local timezone day shift
              const str = typeof endDateRaw === 'string' ? endDateRaw : String(endDateRaw);
              const match = str.match(/(\d{4})-(\d{2})-(\d{2})/);
              if (match) return new Date(parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]));
              const d = new Date(str);
              return new Date(d.getFullYear(), d.getMonth(), d.getDate());
            })()
          : null;

        // Skip if date is before start or after end
        if (dateOnly < startDate) return false;
        if (endDate && dateOnly > endDate) return false;

        const diffMs = dateOnly.getTime() - startDate.getTime();
        const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24));

        switch (item.schedule.repeat) {
          case 'daily':
            return diffDays >= 0;
          case 'weekly':
            return diffDays >= 0 && diffDays % 7 === 0;
          case 'monthly': {
            // Same day-of-month as start date
            return dateOnly.getDate() === startDate.getDate() && diffDays >= 0;
          }
          default:
            return false;
        }
      }
      return false;
    });
  };

  const { daysInMonth, startingDayOfWeek } = getDaysInMonth(currentDate);
  const monthName = currentDate.toLocaleString('default', { month: 'long', year: 'numeric' });

  const previousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1));
  };

  const today = new Date();
  const isToday = (day: number) => {
    return day === today.getDate() &&
           currentDate.getMonth() === today.getMonth() &&
           currentDate.getFullYear() === today.getFullYear();
  };

  const selectedDateItems = selectedDate ? getItemsForDate(selectedDate) : [];

  return (
    <div className="flex-1 flex flex-col bg-gray-50 min-h-0">
      {error && (
        <div className="flex items-center gap-3 px-4 py-3 bg-red-50 border-b border-red-200 text-red-700 text-sm">
          <i className="fas fa-circle-exclamation shrink-0"></i>
          <span className="flex-1">{error}</span>
          <button onClick={() => setError(null)} className="shrink-0 hover:text-red-900">
            <i className="fas fa-times"></i>
          </button>
        </div>
      )}
      <Header title="Calendar" />

      <div className="flex-1 overflow-auto p-4 sm:p-6">
        <div className="max-w-6xl mx-auto">
          <div className="bg-white rounded-xl p-4 sm:p-6 shadow-sm border border-gray-100 mb-4">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-dark">{monthName}</h2>
              <div className="flex gap-2">
                <button
                  onClick={previousMonth}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <i className="fas fa-chevron-left"></i>
                </button>
                <button
                  onClick={() => setCurrentDate(new Date())}
                  className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90"
                >
                  Today
                </button>
                <button
                  onClick={nextMonth}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <i className="fas fa-chevron-right"></i>
                </button>
              </div>
            </div>

            <div className="grid grid-cols-7 gap-2">
              {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
                <div key={day} className="text-center font-bold text-gray-600 py-2">
                  {day}
                </div>
              ))}

              {Array.from({ length: startingDayOfWeek }).map((_, i) => (
                <div key={`empty-${i}`} className="min-h-[80px]"></div>
              ))}

              {Array.from({ length: daysInMonth }).map((_, i) => {
                const day = i + 1;
                const date = new Date(currentDate.getFullYear(), currentDate.getMonth(), day);
                const dayItems = getItemsForDate(date);
                const isSelected = selectedDate?.getDate() === day &&
                                 selectedDate?.getMonth() === currentDate.getMonth() &&
                                 selectedDate?.getFullYear() === currentDate.getFullYear();

                return (
                  <div
                    key={day}
                    onClick={() => setSelectedDate(date)}
                    className={`min-h-[80px] border rounded-lg p-2 cursor-pointer transition-all hover:border-primary ${
                      isToday(day) ? 'bg-primary/10 border-primary' : 'border-gray-200'
                    } ${isSelected ? 'ring-2 ring-primary' : ''}`}
                  >
                    <div className={`text-sm font-medium mb-1 ${
                      isToday(day) ? 'text-primary' : 'text-gray-700'
                    }`}>
                      {day}
                    </div>
                    {dayItems.length > 0 && (
                      <div className="space-y-1">
                        {dayItems.slice(0, 2).map(item => (
                          <div
                            key={item.id}
                            className={`text-xs px-1 py-0.5 rounded truncate flex items-center gap-1 ${
                              item.type === 'note' ? 'bg-blue-100 text-blue-700' :
                              item.completed ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                            }`}
                          >
                            {item.type === 'note' && <i className="fas fa-note-sticky text-[10px] opacity-60"></i>}
                            {item.type === 'todo' && item.schedule && item.schedule.repeat !== 'none' && (
                              <i className="fas fa-repeat text-[10px] opacity-60" title={`Repeats ${item.schedule.repeat}`}></i>
                            )}
                            <span className="truncate">{item.title}</span>
                          </div>
                        ))}
                        {dayItems.length > 2 && (
                          <div className="text-xs text-gray-500">+{dayItems.length - 2} more</div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {selectedDate && selectedDateItems.length > 0 && (
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <h3 className="text-lg font-bold text-dark mb-4">
                Items for {selectedDate.toLocaleDateString()}
              </h3>
              <div className="space-y-3">
                {selectedDateItems.map(item => (
                  <div
                    key={item.id}
                    className="p-4 border border-gray-200 rounded-lg hover:border-primary transition-colors cursor-pointer"
                    onClick={() => navigate(item.type === 'note' ? `/notes/${item.id}` : `/todos/${item.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        {item.type === 'note' && <i className="fas fa-note-sticky text-blue-500"></i>}
                        <h4 className="font-semibold text-dark">{item.title}</h4>
                      </div>
                      {item.type === 'todo' && (
                        <span className={`text-xs px-2 py-1 rounded ${
                          item.completed ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                        }`}>
                          {item.completed ? 'Completed' : 'Pending'}
                        </span>
                      )}
                      {item.type === 'note' && (
                        <span className="text-xs px-2 py-1 rounded bg-blue-100 text-blue-700">Note</span>
                      )}
                    </div>
                    {item.description && <p className="text-sm text-gray-600 mb-2">{item.description}</p>}
                    <div className="flex items-center gap-2">
                      <span className={`text-xs px-2 py-1 rounded ${
                        item.priority === 'high' ? 'bg-red-100 text-red-700' :
                        item.priority === 'medium' ? 'bg-yellow-100 text-yellow-700' :
                        'bg-gray-100 text-gray-700'
                      }`}>
                        {item.priority}
                      </span>
                      {item.tags.slice(0, 3).map(tag => (
                        <span key={tag} className="text-xs bg-primary/10 text-primary px-2 py-1 rounded">
                          #{tag}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
