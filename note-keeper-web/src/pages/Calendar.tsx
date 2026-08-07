/**
 * @module Calendar
 * @category Pages
 * @description Calendar page — todos with due dates displayed in a monthly calendar grid.
 * Shows completion markers for recurring todos based on completion log.
 */
import React, {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {PriorityBadge} from '../components/PriorityBadge';
import {api} from '../utils/api';
import {useWebSocket} from '../hooks/useWebSocket';
import {TodoCompletionLog} from '../types';

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
  lastCompletedAt?: Date | string;
};

/** Calendar page displaying todos and notes by date in a monthly grid view. */
export const Calendar: React.FC = () => {
  const navigate = useNavigate();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [items, setItems] = useState<CalendarItem[]>([]);
  const [completionLogs, setCompletionLogs] = useState<Map<string, Date[]>>(new Map()); // todoId -> array of completedAt dates
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
          schedule: t.schedule,
          lastCompletedAt: t.lastCompletedAt
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

      // Load completion logs for recurring todos
      const recurringTodos = todos.filter(t => t.schedule && t.schedule.repeat && t.schedule.repeat !== 'none');
      const logsMap = new Map<string, Date[]>();
      await Promise.all(
        recurringTodos.map(async (t) => {
          try {
            const logs = await api.todos.getCompletionLog(t.id);
            if (logs && logs.length > 0) {
              logsMap.set(t.id, logs.map(l => new Date(l.completedAt)));
            }
          } catch (e) {
            // Ignore errors for individual todo logs
          }
        })
      );
      setCompletionLogs(logsMap);
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
   * Check if a recurring todo was completed on a specific date.
   */
  const wasCompletedOnDate = (todoId: string, date: Date): boolean => {
    const logs = completionLogs.get(todoId);
    if (!logs) return false;
    return logs.some(logDate => isSameDay(logDate, date));
  };

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
    <PageShell error={error} onDismissError={() => setError(null)}>
      <Header title="Calendar" />

      <div className="flex-1 min-h-0 w-full overflow-y-auto overflow-x-hidden">
        <div className="max-w-6xl mx-auto p-4 sm:p-6">
          <div className="bg-surface rounded-xl p-4 sm:p-6 shadow-sm border border-border mb-4">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-text">{monthName}</h2>
              <div className="flex gap-2">
                <button
                  onClick={previousMonth}
                  className="p-2 hover:bg-hover rounded-lg transition-colors text-text"
                  aria-label="Previous month"
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
                  className="p-2 hover:bg-hover rounded-lg transition-colors text-text"
                  aria-label="Next month"
                >
                  <i className="fas fa-chevron-right"></i>
                </button>
              </div>
            </div>

            <div className="grid grid-cols-7 gap-2">
              {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
                <div key={day} className="text-center font-bold text-text-secondary py-2">
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
                      isToday(day) ? 'bg-primary/10 border-primary' : 'border-border bg-background'
                    } ${isSelected ? 'ring-2 ring-primary' : ''}`}
                  >
                    <div className={`text-sm font-medium mb-1 ${
                      isToday(day) ? 'text-primary' : 'text-text'
                    }`}>
                      {day}
                    </div>
                    {dayItems.length > 0 && (
                      <div className="space-y-1">
                        {dayItems.slice(0, 2).map(item => {
                          // Check if this recurring todo was completed on this date
                          const isRecurring = item.type === 'todo' && item.schedule && item.schedule.repeat !== 'none';
                          const completedOnThisDate = isRecurring && wasCompletedOnDate(item.id, date);
                          return (
                            <div
                              key={item.id}
                              className={`text-xs px-1 py-0.5 rounded truncate flex items-center gap-1 ${
                                item.type === 'note' ? 'bg-blue-500/15 text-blue-400' :
                                completedOnThisDate ? 'bg-green-500/15 text-green-400' :
                                item.completed ? 'bg-green-500/15 text-green-400' : 'bg-yellow-500/15 text-yellow-400'
                              }`}
                            >
                              {item.type === 'note' && <i className="fas fa-note-sticky text-[10px] opacity-60"></i>}
                              {isRecurring && (
                                <i className="fas fa-repeat text-[10px] opacity-60" title={`Repeats ${item.schedule.repeat}`}></i>
                              )}
                              {completedOnThisDate && (
                                <i className="fas fa-check-circle text-[10px] text-green-400" title="Completed on this day"></i>
                              )}
                              <span className="truncate">{item.title}</span>
                            </div>
                          );
                        })}
                        {dayItems.length > 2 && (
                          <div className="text-xs text-text-secondary">+{dayItems.length - 2} more</div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {selectedDate && selectedDateItems.length > 0 && (
            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <h3 className="text-lg font-bold text-text mb-4">
                Items for {selectedDate.toLocaleDateString()}
              </h3>
              <div className="space-y-3">
                {selectedDateItems.map(item => {
                  // Check if this recurring todo was completed on the selected date
                  const isRecurring = item.type === 'todo' && item.schedule && item.schedule.repeat !== 'none';
                  const completedOnSelectedDate = isRecurring && selectedDate && wasCompletedOnDate(item.id, selectedDate);
                  return (
                    <div
                      key={item.id}
                      className="p-4 border border-border rounded-lg hover:border-primary transition-colors cursor-pointer bg-background"
                      onClick={() => navigate(item.type === 'note' ? `/notes/${item.id}` : `/todos/${item.id}`)}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-2">
                          {item.type === 'note' && <i className="fas fa-note-sticky text-secondary"></i>}
                          <h4 className="font-semibold text-text">{item.title}</h4>
                        </div>
                        {item.type === 'todo' && (
                          <span className={`text-xs px-2 py-1 rounded ${
                            completedOnSelectedDate ? 'bg-green-500/15 text-green-400' :
                            item.completed ? 'bg-green-500/15 text-green-400' : 'bg-yellow-500/15 text-yellow-400'
                          }`}>
                            {completedOnSelectedDate ? '✓ Completed' : item.completed ? 'Completed' : 'Pending'}
                          </span>
                        )}
                        {item.type === 'note' && (
                          <span className="text-xs px-2 py-1 rounded bg-blue-500/15 text-blue-400">Note</span>
                        )}
                      </div>
                      {item.description && <p className="text-sm text-text-secondary mb-2">{item.description}</p>}
                      <div className="flex items-center gap-2">
                        <PriorityBadge priority={item.priority} />
                        {item.tags.slice(0, 3).map(tag => (
                          <span key={tag} className="text-xs bg-primary/10 text-primary px-2 py-1 rounded">
                            #{tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>
    </PageShell>
  );
};
