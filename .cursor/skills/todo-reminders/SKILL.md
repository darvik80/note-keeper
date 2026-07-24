---
name: todo-reminders
description: >-
  Debug or change todo Telegram/DingTalk reminders and daily/weekly/monthly
  recurrence. Use when reminders fire once, schedule_repeat stuck, notified_at,
  ReminderService, or catch-up for recurring todos.
---

# Todo reminders

## Root cause pattern (already fixed once)

`markReminderNotified` without advancing `reminder` → `notified_at >= reminder` forever → daily never fires again.

## Checklist

1. Read `ReminderService` + `TodoMapper.xml` (`findWithDueReminders`, `findStuckRecurringReminders`, `advanceRecurringReminder`)
2. Confirm post-notify path calls `advanceRecurringIfNeeded` for `daily|weekly|monthly`
3. Confirm `TodoService.update` clears `notified_at` when reminder changes
4. Inspect DB: `reminder`, `notified_at`, `schedule_repeat`
5. Run `ReminderServiceTest` + reminder-related `TodoServiceTest`

## Key files

- `note-keeper-service/.../service/ReminderService.java`
- `note-keeper-service/.../service/TodoService.java`
- `note-keeper-service/.../mapper/TodoMapper.java` + `resources/mapper/TodoMapper.xml`
- Rule: `.cursor/rules/reminders.mdc`
- Docs: `AGENTS.md` § Reminders

## Note reminders

`note.reminder` is display/calendar only. Do not add notify logic there unless product asks.
