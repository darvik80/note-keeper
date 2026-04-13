import React, { useState, useEffect } from 'react';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Note, Todo } from '../types';

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
    <div className="flex-1 flex flex-col bg-gray-50">
      <Header title="Analytics" />

      <div className="flex-1 overflow-auto p-8">
        <div className="max-w-6xl mx-auto">
          <div className="flex gap-2 mb-6">
            {(['week', 'month', 'year'] as const).map(range => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                className={`px-4 py-2 rounded-lg transition-colors ${
                  timeRange === range
                    ? 'bg-primary text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                {range.charAt(0).toUpperCase() + range.slice(1)}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between mb-2">
                <span className="text-gray-600">Notes Created</span>
                <i className="fas fa-note-sticky text-blue-500 text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-dark">{notesInRange.length}</p>
            </div>

            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between mb-2">
                <span className="text-gray-600">Todos Created</span>
                <i className="fas fa-list-check text-green-500 text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-dark">{todosInRange.length}</p>
            </div>

            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between mb-2">
                <span className="text-gray-600">Completed Todos</span>
                <i className="fas fa-check-circle text-green-500 text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-dark">{completedTodos}</p>
            </div>

            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <div className="flex items-center justify-between mb-2">
                <span className="text-gray-600">Completion Rate</span>
                <i className="fas fa-chart-line text-purple-500 text-xl"></i>
              </div>
              <p className="text-3xl font-bold text-dark">{completionRate}%</p>
            </div>
          </div>

          <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100 mb-6">
            <h3 className="text-lg font-bold text-dark mb-4">Activity Chart</h3>
            <div className="flex items-end gap-1 h-48">
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
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <h3 className="text-lg font-bold text-dark mb-4">Top Tags</h3>
              <div className="space-y-3">
                {topTags.map(([tag, count]) => (
                  <div key={tag} className="flex items-center justify-between">
                    <span className="text-gray-700">#{tag}</span>
                    <div className="flex items-center gap-3">
                      <div className="w-32 bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-primary h-2 rounded-full"
                          style={{ width: `${(count / topTags[0][1]) * 100}%` }}
                        ></div>
                      </div>
                      <span className="text-sm font-medium text-gray-600 w-8 text-right">{count}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <h3 className="text-lg font-bold text-dark mb-4">Priority Distribution</h3>
              <div className="space-y-4">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-red-600 font-medium">High Priority</span>
                    <span className="text-sm font-medium text-gray-600">{priorityCounts.high}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-3">
                    <div
                      className="bg-red-500 h-3 rounded-full"
                      style={{ width: `${(priorityCounts.high / (priorityCounts.high + priorityCounts.medium + priorityCounts.low || 1)) * 100}%` }}
                    ></div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-yellow-600 font-medium">Medium Priority</span>
                    <span className="text-sm font-medium text-gray-600">{priorityCounts.medium}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-3">
                    <div
                      className="bg-yellow-500 h-3 rounded-full"
                      style={{ width: `${(priorityCounts.medium / (priorityCounts.high + priorityCounts.medium + priorityCounts.low || 1)) * 100}%` }}
                    ></div>
                  </div>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-600 font-medium">Low Priority</span>
                    <span className="text-sm font-medium text-gray-600">{priorityCounts.low}</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-3">
                    <div
                      className="bg-gray-500 h-3 rounded-full"
                      style={{ width: `${(priorityCounts.low / (priorityCounts.high + priorityCounts.medium + priorityCounts.low || 1)) * 100}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
