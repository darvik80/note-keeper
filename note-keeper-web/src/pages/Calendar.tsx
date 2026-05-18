/**
 * @module Calendar
 * @category Pages
 * @description Calendar page — todos with due dates displayed in a monthly calendar grid.
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Header } from '../components/Header';
import { api } from '../utils/api';
import { Todo } from '../types';

/** Calendar page displaying todos by due date in a monthly grid view. */
export const Calendar: React.FC = () => {
  const navigate = useNavigate();
  const [currentDate, setCurrentDate] = useState(new Date());
  const [todos, setTodos] = useState<Todo[]>([]);
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        console.log('[Calendar] Loading todos...');
        const t = await api.todos.getAll({ isArchived: false, isDeleted: false });
        console.log('[Calendar] Loaded todos:', t.length);
        // Show todos with dueDate OR reminder
        const withDate = t.filter(todo => todo.dueDate || todo.reminder);
        console.log('[Calendar] Todos with date:', withDate.length);
        setTodos(withDate);
      } catch (err) {
        console.error('[Calendar] Failed to load calendar data:', err);
      }
    };
    load();
  }, []);

  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    return { daysInMonth, startingDayOfWeek };
  };

  const getTodosForDate = (date: Date) => {
    return todos.filter(todo => {
      // Check dueDate
      if (todo.dueDate) {
        const dueDate = new Date(todo.dueDate);
        if (dueDate.getDate() === date.getDate() &&
            dueDate.getMonth() === date.getMonth() &&
            dueDate.getFullYear() === date.getFullYear()) {
          return true;
        }
      }
      // Check reminder
      if (todo.reminder) {
        const reminderDate = new Date(todo.reminder);
        if (reminderDate.getDate() === date.getDate() &&
            reminderDate.getMonth() === date.getMonth() &&
            reminderDate.getFullYear() === date.getFullYear()) {
          return true;
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

  const selectedDateTodos = selectedDate ? getTodosForDate(selectedDate) : [];

  return (
    <div className="flex-1 flex flex-col bg-gray-50 min-h-0">
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
                const dayTodos = getTodosForDate(date);
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
                    {dayTodos.length > 0 && (
                      <div className="space-y-1">
                        {dayTodos.slice(0, 2).map(todo => (
                          <div
                            key={todo.id}
                            className={`text-xs px-1 py-0.5 rounded truncate ${
                              todo.completed ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                            }`}
                          >
                            {todo.title}
                          </div>
                        ))}
                        {dayTodos.length > 2 && (
                          <div className="text-xs text-gray-500">+{dayTodos.length - 2} more</div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {selectedDate && selectedDateTodos.length > 0 && (
            <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
              <h3 className="text-lg font-bold text-dark mb-4">
                Todos for {selectedDate.toLocaleDateString()}
              </h3>
              <div className="space-y-3">
                {selectedDateTodos.map(todo => (
                  <div
                    key={todo.id}
                    className="p-4 border border-gray-200 rounded-lg hover:border-primary transition-colors cursor-pointer"
                    onClick={() => navigate(`/todos/${todo.id}`)}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-semibold text-dark">{todo.title}</h4>
                      <span className={`text-xs px-2 py-1 rounded ${
                        todo.completed ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                      }`}>
                        {todo.completed ? 'Completed' : 'Pending'}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600 mb-2">{todo.description}</p>
                    <div className="flex items-center gap-2">
                      <span className={`text-xs px-2 py-1 rounded ${
                        todo.priority === 'high' ? 'bg-red-100 text-red-700' :
                        todo.priority === 'medium' ? 'bg-yellow-100 text-yellow-700' :
                        'bg-gray-100 text-gray-700'
                      }`}>
                        {todo.priority}
                      </span>
                      {todo.tags.slice(0, 3).map(tag => (
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
