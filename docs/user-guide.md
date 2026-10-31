# NoteKeeper user guide

**English** · [Русский](user-guide.ru.md)

Version **2.0.2**. Notes, todos, and a password vault that you host yourself. The web UI ships inside the server; the Android app talks to the same `/api/v1`.

A public test instance is at [https://note.cogitor.dev](https://note.cogitor.dev). It is there so you can click through the product. Do not store real passwords or private notes on it.

How to build and deploy: [README.md](../README.md) · [на русском](../README.ru.md). Releases: [CHANGELOG.md](../CHANGELOG.md).

![Home](screenshots/home.png)

## Sign in

Open the app URL. Locally that is `http://localhost:8080` (routes live under the hash, so the login screen is `#/login`). On the test stand it is [https://note.cogitor.dev](https://note.cogitor.dev).

![Login](screenshots/login.png)

You can register with email and password, or sign in with Google or Yandex when the server has that provider configured. Buttons appear only if the corresponding client id is set. After you are in, **Settings → Account** can attach Google or Yandex to the same user so either method works next time.

The session JWT lasts 24 hours. On Android, OAuth opens in Custom Tabs and returns through `notekeeper://oauth`. Chrome is the reliable browser here; Edge often swallows the custom-scheme redirect.

## Layout

On a desktop you get a sidebar and a main pane. The theme picker and account menu sit in the page header. On a phone the same sidebar slides in from a hamburger button.

The **Search** control in the header (the magnifier on mobile) opens the command palette: jump to a note, a todo, or another page.

| Group | Pages |
|-------|--------|
| Main | Home, Notes, Todos, Vault |
| Find | Search, Calendar, Analytics |
| Library | Favorites, Templates, Archive, Trash |
| Footer | Settings |

The default theme is **cogitor**. There are thirteen palettes; the choice is stored in the browser, not in Settings.

## Home

Home shows counts for notes, open tasks, favorites, and the archive, plus recent notes and pending todos. The **Notes** and **Todos** actions in the header take you straight to those lists.

## Notes

![Notes](screenshots/notes.png)

**New note** creates an untitled note in the folder you have selected and opens the editor.

The left panel is a folder tree derived from note paths (there is no separate folder table). The root is unfiled notes, or `default`. You can filter by folder, search the list, or show notes that others shared with you. Drag a note onto a folder to move it.

Each card can be favorited, archived, or sent to trash. Encrypted notes show a lock; shared notes show a share icon.

### Editor

![Note editor](screenshots/note-editor.png)

The editor has a title and a Markdown body. A brand-new empty note opens in edit mode; an existing note opens in preview. Changes autosave; **Ctrl/Cmd+S** saves as well.

The toolbar inserts Markdown snippets, attaches files, locks the note (AES-256-GCM on the server), shares it, opens history, and toggles fullscreen. Preview understands GFM (tables, task lists, strikethrough), fenced code, Mermaid, and images — including attachments that need the JWT.

You can set tags, folder, priority, a reminder time (display only — notes do not send pushes), and favorite.

**Share** invites another person by email or name. They must already have an account on this server.

**History** is the clock in the note toolbar, on web and on Android. Each successful server save writes a `created`, `edited`, or `restored` row. Tap a row to preview that Markdown. Restore writes that body as the current note and adds a `restored` row. Android only has history for notes that have already been synced; a purely local draft does not yet.

**Import** accepts `.txt` and `.md` into the current folder. PDF and Office files are not parsed.

Encrypted notes depend on `app.encryption.key` / `APP_ENCRYPTION_KEY`. If that key changes, those notes cannot be read.

## Todos

![Todos](screenshots/todos.png)

**New todo** opens the editor. You can mark a todo complete from the card. Filter the list by completed versus open, or show items shared with you.

### Editor and schedule

A todo has a title, a Markdown description, tags, priority, attachments, and sharing. You can set a due date, a start, and a reminder datetime.

Repeat can be none, daily, weekly, monthly, weekdays, or a custom set of weekdays. A repeating todo needs a due date or a reminder as the anchor. **Completed** on a recurring todo means done for the **current period only**; the next period it is open again. Past completions live in the completion log.

Reminders go to Telegram and/or DingTalk if you configured them. Sending a notification does **not** move the reminder time — completing the todo (or the nightly rollover) does.

If integrations are on, the todo editor can send a one-off test message.

## Vault

![Vault](screenshots/vault.png)

The vault is folders and items on your account (`/api/v1/vault`). Item types are password, SSH key, API token, and secure note.

**New item** uses the folder selected on the left (`New in …`). To move an item, pick a folder on the row, drag the grip onto a folder, or set Folder in Edit.

You can copy a password or download a PEM for keys. Android can copy the secret to the clipboard.

This is **server-side** storage behind JWT, not a local SQLCipher vault. The Android cache for vault items is not encrypted at rest yet.

## Search, calendar, analytics

![Search](screenshots/search.png)

**Search** runs full-text across notes and todos. You can restrict it to one or the other and save a query for later.

**Calendar** is a month grid of todos (due date or reminder) and notes that have a reminder. Recurring completions mark past days.

![Calendar](screenshots/calendar.png)

**Analytics** shows counts, tags, how priority is mixed, and activity for the last week, month, or year.

## Library

| Page | What it holds |
|------|----------------|
| Favorites | Starred notes and todos |
| Templates | Named Markdown snippets. **Use** creates a note with that body and tags |
| Archive | Soft-parked notes and todos; restore from here |
| Trash | Soft-deleted items; restore or delete permanently |

## Settings

![Settings](screenshots/settings.png)

| Tab | |
|-----|--|
| Account | Profile; link or unlink Google and Yandex |
| Integrations | Telegram bot token + chat ID; DingTalk webhook + optional HMAC secret. You can send a test. |
| Daily report | Time (local, stored as UTC), channels, templates |
| API tokens | Personal tokens `nk_…` with scopes `notes`, `todos`, `vault`, `mcp` |
| API docs | Link to Swagger `/swagger-ui.html` |
| Backup | Cron (default 02:00), retention, export / import / list |

Theme stays in the header, not in Settings.

## Android

Package `dev.cogitor.notekeeper.v2`, minSdk 31. Point the client at your server URL (HTTPS in production), or at [https://note.cogitor.dev](https://note.cogitor.dev) to try the test stand.

Bottom navigation is **Notes | Todos | Vault | Search**. The gear on every tab opens **Settings**: theme, notifications, Telegram/DingTalk, backup, API tokens, and **Log out**.

Pull down to sync. Notes and todos also sync in the background (Room + WorkManager).

List chips: notes **All | Archive | Trash**, then folder chips from `folder`/`subfolder` (the same paths as the web). Todos **Active | Done | Archive | Trash**. Swipe a note left to archive (snackbar **Undo**). Swipe a todo right for done, left for archive. In Archive or Trash, swipe restores. The FAB is only on the Active list so it does not cover archived cards. A new note lands in the folder you have selected.

Vault chips are **All** plus server folders (`GET /vault/folders`). Type a name and tap plus to create one. A new secret inherits the selected folder; the editor can move it later.

Back (system or the arrow) saves the open note or todo. An empty title with a body becomes **Untitled**. Empty new drafts are discarded.

In the note editor, pencil and eye in the top bar switch edit and preview. Existing notes open in Preview (rendered Markdown); new notes open in Edit. The amber check FAB saves (snackbar **Saved**); notes also autosave after about 800 ms. The keyboard resizes the editor instead of panning the title off-screen. Overflow **⋮** has Share, History, Attach, and Trash. Tags suggest values you already use (`GET /tags` plus the local cache); space, comma, or Done commits a chip. The folder field accepts `Work/Q1` and suggests known paths.

Sign-in: email/password, Google, Yandex.

More: [android/README.md](../android/README.md).

## API and other clients

Send `Authorization: Bearer <jwt or nk_ token>`. Interactive docs live at `/swagger-ui.html`.

MCP is `POST /mcp` (token scope `mcp`). FAR Manager plugins are a separate repository — see `far-vault/README.md`.

## Keyboard and palette

Global shortcut bindings were removed. Use **Search** in the header for the command palette (new note, new todo, navigation, title search). Editors still handle **Ctrl/Cmd+S** and **Esc** (leave fullscreen).

## Mobile web

![Notes on a phone](screenshots/notes-mobile.png)

It is the same app. Navigation is a drawer, tap targets are larger, and the command palette opens from the magnifier at the top right.
