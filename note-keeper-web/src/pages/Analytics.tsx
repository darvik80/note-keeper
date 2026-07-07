/**
 * @module Analytics
 * @category Pages
 * @description Analytics page — usage statistics over a selected time range.
 */
import React, {useEffect, useState} from 'react';
import {Header} from '../components/Header';
import {PageShell} from '../components/PageShell';
import {api} from '../utils/api';
import {Note, Todo} from '../types';

/** Analytics page showing note/todo creation counts, completion rate, top tags, priority distribution, and daily activity. */
export const Analytics: React.FC = () => {
  const [notes, setNotes] = useState<Note[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [timeRange, setTimeRange] = useState<'week' | 'month' | 'year'>('week');

  useEffect(() => {
    const load = async () => {
      try {
        const [n, t] = await Promise.all([
          api.notes.getAll(),
          api.todos.getAll()
        ]);
        setNotes(n);
        setTodos(t);
      } catch (err) {
        console.error('Failed to load analytics data', err);
      }
    };
    load();
  }, []);

  const getDateRange = () => {
    const now = new Date();
    const start = new Date();
    if (timeRange === 'week') start.setDate(now.getDate() - 7);
    else if (timeRange === 'month') start.setMonth(now.getMonth() - 1);
    else start.setFullYear(now.getFullYear() - 1);
    return { start, end: now };
  };

  const { start, end } = getDateRange();

  const notesInRange = notes.filter(n => {
    const created = new Date(n.createdAt);
    return created >= start && created <= end;
  });

  const todosInRange = todos.filter(t => {
    const created = new Date(t.createdAt);
    return created >= start && created <= end;
  });

  const completedTodos = todosInRange.filter(t => t.completed).length;
  const completionRate = todosInRange.length > 0 ? (completedTodos / todosInRange.length * 100).toFixed(1) : 0;

  const tagCounts = [...notes, ...todos].reduce((acc, item) => {
    item.tags.forEach(tag => {
      acc[tag] = (acc[tag] || 0) + 1;
    });
    return acc;
  }, {} as Record<string, number>);

  const topTags = Object.entries(tagCounts)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 10);

  const priorityCounts = {
    high: [...notes, ...todos].filter(item => item.priority === 'high').length,
    medium: [...notes, ...todos].filter(item => item.priority === 'medium').length,
    low: [...notes, ...todos].filter(item => item.priority === 'low').length
  };

  const getDailyActivity = () => {
    const days = timeRange === 'week' ? 7 : timeRange === 'month' ? 30 : 365;
    const activity = Array(days).fill(0);

    [...notesInRange, ...todosInRange].forEach(item => {
      const created = new Date(item.createdAt);
      const daysDiff = Math.floor((end.getTime() - created.getTime()) / (1000 * 60 * 60 * 24));
      if (daysDiff >= 0 && daysDiff < days) {
        activity[days - 1 - daysDiff]++;
      }
    });

    return activity;
  };

  const dailyActivity = getDailyActivity();
  const maxActivity = Math.max(...dailyActivity, 1);

  return (
    <PageShell>
      <Header title="Analytics" />

      <div className="flex-1 min-h-0 w-full overflow-y-auto overflow-x-hidden">
        <div className="max-w-6xl mx-auto p-4 lg:p-8">
          <div className="flex gap-2 mb-6">
            {(['week', 'month', 'year'] as const).map(range => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                className={`px-4 py-2 rounded-lg transition-colors ${
                  timeRange === range
                    ? 'bg-primary text-white'
                    : 'btn-secondary border border-border'
                }`}
              >
                {range.charAt(0).toUpperCase() + range.slice(1)}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <div className="flex items-center justify-between mb-2">
                <span className="text-text-secondary">Notes Created</span>
                <i className="fas fa-note-sticky text-secondary text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-text">{notesInRange.length}</p>
            </div>

            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <div className="flex items-center justify-between mb-2">
                <span className="text-text-secondary">Todos Created</span>
                <i className="fas fa-list-check text-primary text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-text">{todosInRange.length}</p>
            </div>

            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <div className="flex items-center justify-between mb-2">
                <span className="text-text-secondary">Completed Todos</span>
                <i className="fas fa-check-circle text-primary text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-text">{completedTodos}</p>
            </div>

            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <div className="flex items-center justify-between mb-2">
                <span className="text-text-secondary">Completion Rate</span>
                <i className="fas fa-chart-line text-purple-400 text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-text">{completionRate}%</p>
            </div>
          </div>

          <div className="bg-surface rounded-xl p-6 shadow-sm border border-border mb-6">
            <h3 className="text-lg font-bold text-text mb-4">Activity Chart</h3>
            <div className="flex items-end gap-1 h-48 bg-background rounded-lg p-3">
              {dailyActivity.map((count, i) => (
                <div
                  key={i}
                  className="flex-1 bg-primary rounded-t transition-all hover:bg-primary/80"
                  style={{ height: `${(count / maxActivity) * 100}%`, minHeight: count > 0 ? '4px' : '0' }}
                  title={`${count} items`}
                ></div>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <h3 className="text-lg font-bold text-text mb-4">Top Tags</h3>
              <div className="space-y-3">
                {topTags.map(([tag, count]) => (
                  <div key={tag} className="flex items-center justify-between">
                    <span className="text-text">#{tag}</span>
                    <div className="flex items-center gap-3">
                      <div className="w-32 bg-hover rounded-full h-2">
                        <div
                          className="bg-primary h-2 rounded-full"
                          style={{ width: `${(count / topTags[0][1]) * 100}%` }}
                        ></div>
                      </div>
                      <span className="text-sm font-medium text-text-secondary w-8 text-right">{count}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-surface rounded-xl p-6 shadow-sm border border-border">
              <h3 className="text-lg font-bold text-text mb-4">Priority Distribution</h3>
              <div className="space-y-4">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-red-400 font-medium">High Priority</span>
                    <span className="text-sm font-medium text-text-secondary">{priorityCounts.high}</span>
                  </div>
                  <div className="w-full bg-hover rounded-full h-3">
                    <div
                      className="bg-red-500 h-3 rounded-full"
                      style={{ width: `${(priorityCounts.high / (priorityCounts.high + priorityCounts.medium + priorityCounts.low || 1)) * 100}%` }}
                    ></div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-yellow-400 font-medium">Medium Priority</span>
                    <span className="text-sm font-medium text-text-secondary">{priorityCounts.medium}</span>
                  </div>
                  <div className="w-full bg-hover rounded-full h-3">
                    <div
                      className="bg-yellow-500 h-3 rounded-full"
                      style={{ width: `${(priorityCounts.medium / (priorityCounts.high + priorityCounts.medium + priorityCounts.low || 1)) * 100}%` }}
                    ></div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-text-secondary font-medium">Low Priority</span>
                    <span className="text-sm font-medium text-text-secondary">{priorityCounts.low}</span>
                  </div>
                  <div className="w-full bg-hover rounded-full h-3">
                    <div
                      className="bg-green-500/60 h-3 rounded-full"
                      style={{ width: `${(priorityCounts.low / (priorityCounts.high + priorityCounts.medium + priorityCounts.low || 1)) * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageShell>
  );
};
