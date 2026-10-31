# Changelog

## 2.0.3 — 2026-10-31

- Monitoring: public `GET /actuator/prometheus` with JVM, HTTP, Tomcat and Hikari metrics
- Business metrics: notes added/archived, logins and auth rejections, Telegram webhook errors, reminders sent, backup results and duration, `notekeeper_records_*` gauges
- HTTP latency histograms and SLO buckets for `http_server_requests_seconds`
- .deb now carries the GraalVM native binary (no JVM) and writes its config on install
- .deb upgrade keeps the existing `application.yaml` and restarts the service
- OAuth redirects use the HTTPS public URL; native builds bundle every JS chunk
- Web: save button feedback, note history preview, drag notes and secrets onto folders, vault card layout
- Android: folders, archive survives sync, note history preview, tag suggestions
- Docs: Prometheus scrape guide, Russian user guide

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
