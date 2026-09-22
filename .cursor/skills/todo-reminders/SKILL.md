---
name: todo-reminders
description: >-
  Debug or change todo Telegram/DingTalk reminders and daily/weekly/monthly
  recurrence. Use when reminders fire once, schedule_repeat stuck, notified_at,
  ReminderService, completed strikethrough on a new day, or catch-up for
  recurring todos.
---

# Todo reminders

## Model (habit series)

`completed` = current period only. `reminder` is the next fire and the clock-time template.
`Recurrence.rolloverIfNeeded` aligns state to today and never moves `reminder` earlier than the stored value. Notify does **not** advance reminder.

## Checklist

1. Read `Recurrence.java` + `ReminderService` + `TodoService.toggleComplete`
2. Confirm rollover on `findAll` / `findById` / scheduler
3. Confirm notify only sets `notified_at`
4. Inspect DB: `reminder`, `notified_at`, `completed`, `last_completed_at`, `schedule_repeat`, `todo_completion_log`
5. UI: TodoCard shows last + next; strikethrough only if `completed` after rollover
6. Run `RecurrenceTest`, `ReminderServiceTest`, `TodoServiceTest`

## Key files

- `note-keeper-service/.../service/Recurrence.java`
- `note-keeper-service/.../service/ReminderService.java`
- `note-keeper-service/.../service/TodoService.java`
- `note-keeper-web/src/components/TodoCard.tsx`
- Rule: `.cursor/rules/reminders.mdc`
- Docs: `AGENTS.md` § Reminders

## Note reminders

`note.reminder` is display/calendar only. Do not add notify logic there unless product asks.
