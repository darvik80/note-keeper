# Changelog

## 2.0.2 — 2026-10-27

- User guide and screenshots (`docs/user-guide.md`)
- Android OAuth: one-time `code` + `state`; JWT no longer in deep links or `/oauth/app` URLs
- Android: HTTP body logging off; JWT/DB excluded from cloud backup
- Android: Active shows open todos only; Done shows completed only
- Android: IME resizes the editor instead of panning typed text off-screen
- Android: list cards no longer show a double frame; swipe clipped to the card radius
- Android: Google / Yandex sign-in prefers Chrome Custom Tabs and an HTTPS `intent://` bridge so Edge does not steal the redirect
- Android launcher uses a NoteKeeper folded-note mark
- Current Android app lives in `android/` (old client removed)
- Auth: link Google and Yandex on one account
- Web: Settings → Shortcuts removed; command palette stays
- Mermaid diagrams no longer tear down the React tree
- Native: start on older SQLite DBs that lack `yandex_id`

## 2.0.1 — 2026-10-27

- Cogitor theme is the default (13 palettes total)
- Images in note Markdown render in the web editor
- Version bump to 2.0.1

## 2.0.0 — 2026-10-27

Supported in 2.0:

- Notes and todos (Markdown, folders, tags, archive, trash, share, history)
- Recurring todos and Telegram/DingTalk reminders + daily reports
- Password vault (web and Android)
- Search, calendar, analytics, templates, attachments, backup
- Email/password, Google, Yandex
- Personal API tokens (`nk_…`, scopes `notes` / `todos` / `vault` / `mcp`)
- Android Compose app (minSdk 31)
- SQLite or PostgreSQL; JVM or GraalVM native; Docker
